from tqdm import tqdm

from parsers.asat_usage_extractor import ASATUsageExtractor
from util.downloader import RepoDownloader
from model.db import DB
from util.statistics import compute_statistics

DB_PATH = 'data/projects.sqlite3'
REPOS_PATH = 'repos'

db = DB(DB_PATH)
asats = db.get_ASATs()
projects = db.get_projects()

asat_usage_extractor = ASATUsageExtractor(asats)
downloader = RepoDownloader(REPOS_PATH)

for project in tqdm(projects):
    repo_path = downloader.download(project.url)
    project.asat_usages = asat_usage_extractor.extract(repo_path)

cloud_projects = [proj for proj in projects if proj.is_cloud_app]
non_cloud_projects = [proj for proj in projects if not proj.is_cloud_app]

print('Statistics for cloud projects:')
compute_statistics(cloud_projects, asats)
print('Statistics for non-cloud projects:')
compute_statistics(non_cloud_projects, asats)
