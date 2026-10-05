import hashlib

KEY_PREFIX = "cdr_"


def hash_key(key: str) -> str:
    # Must match the user service, which creates the keys.
    return hashlib.sha256(key.encode("utf-8")).hexdigest()
