from enum import Enum


class Status(Enum):
    CREATED = 'created'
    PROCESSING = 'processing'
    SUCCESS = 'success'
    FAILED = 'failed'


