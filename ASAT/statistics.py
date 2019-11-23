from typing import List
import matplotlib.pyplot as plt
from model.project import Project


def get_asat_usage_numbers(projects: List[Project]):
    """Get number of projects per ASAT."""
    numbers = {}
    for project in projects:
        for asat_usage in project.asat_usages:
            asat = asat_usage.asat
            if asat not in numbers:
                numbers[asat] = 0
            numbers[asat] += 1

    return numbers


def plot_asat_usage_percentages(projects):
    numbers = get_asat_usage_numbers(projects)
    n_projects = len(projects)

    group_data = [(num / n_projects)*100 for num in numbers.values()]
    group_names = list(numbers.keys())

    fig, ax = plt.subplots()
    ax.barh(group_names, group_data)
    ax.set(xlabel='Percentage',
           ylabel='ASAT',
           title='Percentages for ASATs used in projects')
    plt.show()
