from typing import List
import matplotlib.pyplot as plt
from model.project import Project
import numpy as np


def get_asat_usage_numbers(projects: List[Project]):
    """Get number of projects per ASAT."""
    numbers = {}
    for project in projects:
        for asat_usage in project.asat_usages:
            asat = asat_usage.asat.name
            if asat not in numbers:
                numbers[asat] = 0
            numbers[asat] += 1

    return numbers


def get_asat_category_usage_numbers(projects: List[Project]):
    """Get number of projects per ASAT category."""
    numbers = {}
    for project in projects:
        for asat_usage in project.asat_usages:
            category = asat_usage.asat.category
            if category is not None:
                if category not in numbers:
                    numbers[category] = set()
                numbers[category].add(project.url)

    return {c: len(numbers[c]) for c in numbers}


def plot_asat_usage_percentages(asats, projects):
    numbers = get_asat_usage_numbers(projects)
    n_projects = len(projects)

    for asat in asats:
        if asat.name not in numbers:
            numbers[asat.name] = 0

    plot(n_projects, numbers)


def plot_asat_category_usage_percentages(asats, projects):
    numbers = get_asat_category_usage_numbers(projects)
    n_projects = len(projects)

    categories = {asat.category for asat in asats if asat.category is not None}
    for category in categories:
        if category not in numbers:
            numbers[category] = 0

    plot(n_projects, numbers, (15, 2))


def plot(n_projects, numbers, figsize=(15, 15)):
    sorted_numbers = {k: numbers[k] for k in sorted(numbers)}
    group_data = [(num / n_projects) * 100 for num in sorted_numbers.values()]
    group_names = list(sorted_numbers.keys())
    fig, ax = plt.subplots(figsize=figsize)
    ax.barh(group_names, group_data)
    ax.set(xlabel='Percentage',
           ylabel='ASAT',
           title='Percentages for ASATs used in projects')
    ax.xaxis.set_ticks(np.arange(0, 110, 10))
    for i, v in enumerate(group_data):
        ax.text(v + 1, i - .15, str(round(v, 0)), color='blue')
    plt.show()


def print_average_number_of_asats(projects):
    total = 0
    for project in projects:
        total += len(project.asat_usages)

    print('Average number of ASATs used by projects', total/len(projects))


def print_asat_arg_usage(projects: List[Project]):
    param_numbers = {}
    asat_numbers = {}
    for project in projects:
        for asat_usage in project.asat_usages:
            asat = asat_usage.asat.name
            if asat not in asat_numbers:
                asat_numbers[asat] = 0
            asat_numbers[asat] += 1

            if asat not in param_numbers:
                param_numbers[asat] = {}

            arg_usage = asat_usage.arg_usage
            for param in arg_usage.get_parameters():
                if param not in param_numbers[asat]:
                    param_numbers[asat][param] = 0

                param_numbers[asat][param] += 1

    for asat in sorted(param_numbers):
        print('ASAT: ', asat)
        asat_num = asat_numbers[asat]
        for param in param_numbers[asat]:
            param_num = int(param_numbers[asat][param])
            print(f'\tParameter: {param}, {param_num}/{asat_num}')


def compute_statistics(projects, asats):
    print('Number of projects: ', len(projects))
    plot_asat_usage_percentages(asats, projects)
    plot_asat_category_usage_percentages(asats, projects)
    print_average_number_of_asats(projects)
    print_asat_arg_usage(projects)
    print()
