#!/bin/bash
# Build a Lambda zip for a FastAPI service and push it to its function.
# Usage: ./deploy_lambda.sh user|problem
set -euo pipefail

SERVICE="${1:?usage: $0 user|problem}"
FUNCTION="codear-${SERVICE}"
HERE="$(cd "$(dirname "$0")" && pwd)"
SRC="${HERE}/${SERVICE}"
BUILD="$(mktemp -d)"
trap 'rm -rf "$BUILD"' EXIT

# Wheels must match the Lambda runtime (python3.12 on x86_64), not this machine.
pip install --quiet --target "$BUILD/pkg" --implementation cp --python-version 3.12 --only-binary=:all: \
    --platform manylinux2014_x86_64 --platform manylinux_2_17_x86_64 --platform manylinux_2_28_x86_64 \
    -r "${SRC}/requirements.txt"
# The Lambda runtime already ships boto3; bundling it would take the zip near the 50 MB upload limit.
rm -rf "$BUILD"/pkg/{boto3,botocore,s3transfer}*
cp -r "${SRC}/app" "$BUILD/pkg/"
find "$BUILD/pkg" -name '__pycache__' -type d -prune -exec rm -rf {} +
# /var/task is read-only, so without bundled bytecode every cold start recompiles fastapi/sqlalchemy/pydantic
# from source (~2 s). The .pyc files must come from the same Python minor version as the runtime.
python3.12 -W ignore::SyntaxWarning -m compileall -q -j 0 --invalidation-mode unchecked-hash "$BUILD/pkg"
(cd "$BUILD/pkg" && zip -qr "$BUILD/${FUNCTION}.zip" .)

aws lambda update-function-code --function-name "$FUNCTION" --zip-file "fileb://$BUILD/${FUNCTION}.zip" \
    --query 'LastUpdateStatus' --output text
aws lambda wait function-updated-v2 --function-name "$FUNCTION"

# Apply any new tables/columns once, instead of on every cold start.
aws lambda invoke --function-name "$FUNCTION" --cli-binary-format raw-in-base64-out \
    --payload '{"setup_schema": true}' /dev/stdout --query 'StatusCode' --output text
