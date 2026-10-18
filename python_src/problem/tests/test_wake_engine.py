from unittest.mock import MagicMock, patch

from app.core import sqs


def _reset(monkeypatch, instance_id):
    monkeypatch.setattr(sqs, "ENGINE_INSTANCE_ID", instance_id)
    monkeypatch.setattr(sqs, "_last_wake", 0.0)
    ec2 = MagicMock()
    monkeypatch.setattr(sqs, "_ec2_client", ec2)
    return ec2


def test_no_instance_configured_means_no_call(monkeypatch):
    ec2 = _reset(monkeypatch, None)
    sqs.wake_engine()
    ec2.start_instances.assert_not_called()


def test_enqueue_starts_the_engine_once_per_minute(monkeypatch):
    ec2 = _reset(monkeypatch, "i-123")
    with patch.object(sqs.sqs_client, "send_message"):
        sqs.send_to_queue({"a": 1}, queue_url="https://sqs/x")
        sqs.send_to_queue({"a": 2}, queue_url="https://sqs/x")
    ec2.start_instances.assert_called_once_with(InstanceIds=["i-123"])


def test_wakes_again_after_the_throttle_window(monkeypatch):
    ec2 = _reset(monkeypatch, "i-123")
    sqs.wake_engine()
    monkeypatch.setattr(sqs, "_last_wake", sqs._last_wake - sqs.WAKE_EVERY_SECONDS - 1)
    sqs.wake_engine()
    assert ec2.start_instances.call_count == 2


def test_a_failed_start_does_not_fail_the_submission(monkeypatch):
    ec2 = _reset(monkeypatch, "i-123")
    ec2.start_instances.side_effect = RuntimeError("IncorrectInstanceState: stopping")
    with patch.object(sqs.sqs_client, "send_message") as send:
        sqs.send_to_queue({"a": 1}, queue_url="https://sqs/x")
    send.assert_called_once()
