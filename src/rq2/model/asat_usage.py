from dataclasses import dataclass, field
from typing import Optional, Set
from model.asat import ASAT
from model.arg_usage import ArgUsage


@dataclass
class ASATUsage:
    asat: ASAT
    cfg_path: Optional[str] = None
    files: Set[str] = field(default_factory=set)
    arg_usage: ArgUsage = field(default_factory=ArgUsage)

    def is_configured(self):
        has_options = len(self.arg_usage.options)
        has_named = len(self.arg_usage.named)
        return has_options or has_named
