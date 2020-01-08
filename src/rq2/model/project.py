from dataclasses import dataclass
from typing import List

from model.asat_usage import ASATUsage


@dataclass
class Project:
    url: str
    description: str
    stars: int
    commits: int
    asat_usages: List[ASATUsage]
    is_cloud_app: bool
