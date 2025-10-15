from __future__ import annotations

import logging
from logging.handlers import QueueHandler, QueueListener
from queue import Queue


_log_queue: "Queue[logging.LogRecord]" = Queue(maxsize=1000)


def configure_logging(level: int = logging.INFO) -> QueueListener:
    """Configure stdlib logging with a non-blocking queue and an optional Rich handler.

    Returns the QueueListener so callers can stop it on shutdown.
    """
    try:
        from rich.logging import RichHandler  # type: ignore

        sink: logging.Handler = RichHandler(show_time=True, rich_tracebacks=True)
    except Exception:
        sink = logging.StreamHandler()

    root = logging.getLogger()
    root.setLevel(level)

    queue_handler = QueueHandler(_log_queue)
    queue_handler.setLevel(level)

    root.handlers.clear()
    root.addHandler(queue_handler)

    listener = QueueListener(_log_queue, sink)
    listener.start()
    return listener


