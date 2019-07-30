#!/bin/bash

# -----------------------------------------------------------------------------
# Run mvn site and add output as new commit to branch 'gh-pages'
#
# Usage:
# src/main/scripts/gh-pages.sh
# -----------------------------------------------------------------------------

set -e # Fail on error
set -u # Fail on uninitialized

if [ ! -f pom.xml ]; then
    echo "Must run from root directory (containing file 'pom.xml')"
    exit 1
fi

# Save the version
version=$(mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate \
    -Dexpression=project.version 2> /dev/null | egrep -v '^\[' | tail -1)
revision=$(git rev-parse --short=7 HEAD)
if ! git diff-index --quiet HEAD -- ; then
    revision="${revision} (uncommitted changes)"
fi

echo "Will build site and commit to branch 'gh-pages'..."

# Save info about repo and user
localrepo=$(pwd)
email=$(git config user.email)
name=$(git config user.name)

# Now build the site. Unfortunately, need to invoke 'site' separately.
# Otherwise, generated sources would be missed.
mvn clean install
mvn site site:stage

# Create a new git repository for later force-push to gh-pages branch
cd target/staging
git init
git checkout -b gh-pages
git config --local user.email "${email}"
git config --local user.name "${name}"

# Tell GitHub not to run the page through Jekyll
touch .nojekyll

# Add site and commit
git add --force --all
git commit -m "Site for version '${version}', revision '${revision}'."
git push "${localrepo}" +gh-pages

echo "Successfully replaced branch 'gh-pages'."
