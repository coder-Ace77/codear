"""Adds columns that were introduced after the tables were first created.

`Base.metadata.create_all` creates missing tables but never alters existing ones, and the engine (Hibernate)
adds its own columns when it starts. Doing the same here, idempotently, means the API and the engine can be
deployed in either order without one of them failing on a column the other has not added yet.
"""
import logging

from sqlalchemy import text

logger = logging.getLogger(__name__)

# (table, column, SQL type)
COLUMNS = (
    ("submissions", "verdict", "VARCHAR(40)"),
    ("submissions", "failed_test", "INTEGER"),
    ("problems", "checker", "VARCHAR(40)"),
    ("problems", "checker_tolerance", "DOUBLE PRECISION"),
)


def ensure_columns(engine) -> None:
    for table, column, sql_type in COLUMNS:
        try:
            with engine.begin() as connection:
                connection.execute(text(f"ALTER TABLE {table} ADD COLUMN IF NOT EXISTS {column} {sql_type}"))
        except Exception as e:  # another process adding it at the same moment, or no permission
            logger.warning("could not ensure column %s.%s: %s", table, column, e)
