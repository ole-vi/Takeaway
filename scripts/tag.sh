#!/usr/bin/env bash
# Tag last commit as 'latest'.

if [ "$TRAVIS_BRANCH" = "master" -a "$TRAVIS_PULL_REQUEST" = "false" ] -o [ "$TRAVIS_BRANCH" = "dev" -a "$TRAVIS_PULL_REQUEST" = "true" ]; then
  git config --global user.email "leomaxi@outlook.com"
  git config --global user.name "leomaxi"
  # Is this not a build which was triggered by setting a new tag?
    if [ -z "$TRAVIS_TAG" ] -o [ "$TRAVIS_BRANCH" = "dev" ]  ; then
      git remote add release "https://${GH_TOKEN}@github.com/open-learning-exchange/myplanet.git"
      if [ "$TRAVIS_BRANCH" = "dev" ]  ; then
        PACKAGE_VERSION=$(sed -n 's/.*name="app_version">\([^"]*\).*<\/string>/\1/p' < app/src/main/res/values/versions.xml)
        $PACKAGE_VERSION = "${PACKAGE_VERSION}_dev";
      else
        PACKAGE_VERSION=$(sed -n 's/.*name="app_version">\([^"]*\).*<\/string>/\1/p' < app/src/main/res/values/versions.xml)
      fi
#       git push -d release latest
#       git tag -d latest
      git tag -a v${PACKAGE_VERSION} -m "$TRAVIS_COMMIT_MESSAGE"
      git push release --tags
      echo -e "Done with tags.\n"
    fi
fi
