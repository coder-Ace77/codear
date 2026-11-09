from app.core import schema_upgrade


def test_a_failing_alter_does_not_stop_the_service_starting():
    class BrokenEngine:
        def begin(self):
            raise RuntimeError("no permission")

    schema_upgrade.ensure_columns(BrokenEngine())
