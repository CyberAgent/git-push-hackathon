# github client
[![CircleCI](https://circleci.com/gh/halu5071/git-push-hackathon.svg?style=svg)](https://circleci.com/gh/halu5071/git-push-hackathon)  
This is an android application for git-push-hackathon.

# To Build
Please ready for `gradle.properties` file on `app` directory, then add github Client ID, Client Secret, OAuth scheme and OAuth host on it, like this.

```
// app/gradle.properties

CLIENT_ID="hogehoge"
CLIENT_SECRET="fugafuga"
OAUTH_SCHEME="hogefuga"
OAUTH_HOST="fugahoge"
```

<img src="https://github.com/halu5071/git-push-hackathon/blob/master/assets/gradle.png?raw=true" />

build.gradle on app read these values and generate BuildConfig variants.
