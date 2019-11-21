from attr import dataclass
from typing import Optional, Set
from model.asat import ASAT


@dataclass
class ASATUsage:
    asat: ASAT
    cfg_path: Optional[str]
    files: Set[str]
    args: Set[str]
