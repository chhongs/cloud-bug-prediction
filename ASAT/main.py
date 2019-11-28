from tqdm import tqdm

from asat_usage_extractor import ASATUsageExtractor
from downloader import RepoDownloader
from db import DB
from statistics import \
    plot_asat_usage_percentages, \
    print_average_number_of_asats, \
    print_asat_arg_usage

DB_PATH = 'projects.sqlite3'
REPOS_PATH = 'repos'

db = DB(DB_PATH)
asats = db.get_ASATs()
projects = db.get_projects()

asat_usage_extractor = ASATUsageExtractor(asats)
downloader = RepoDownloader(REPOS_PATH)

for project in tqdm(projects):
    repo_path = downloader.download(project.url)
    project.asat_usages = asat_usage_extractor.extract(repo_path)

plot_asat_usage_percentages(asats, projects)
print_average_number_of_asats(projects)
print_asat_arg_usage(projects)
