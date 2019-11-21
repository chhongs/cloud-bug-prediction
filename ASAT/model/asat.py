from attr import dataclass
from typing import List


@dataclass
class ASAT:
    name: str
    description: str
    docs: str
    configs: List[str]
    command: str
