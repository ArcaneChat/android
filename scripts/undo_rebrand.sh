#!/bin/sh

find ./src/main/assets/help/ -type f -name '*.html' | xargs sed -i 's/arcanechat.me\/privacy/delta.chat\/en\/gdpr/g'
find ./src/main/assets/help/ -type f -name '*.html' | xargs sed -i 's/github.com\/ArcaneChat/get.delta.chat/g'
find ./src/main/assets/help/ -type f -name '*.html' | xargs sed -i 's/ArcaneChat/Delta Chat/g'

find ./src/ -type f -name 'strings.xml' | xargs sed -i 's/github.com\/ArcaneChat/get.delta.chat/g'
find ./src/ -type f -name 'strings.xml' | xargs sed -i 's/arcanechat.me\/#contribute/delta.chat\/donate/g'
find ./src/ -type f -name 'strings.xml' | xargs sed -i 's/ArcaneChat/Delta Chat/g'
find ./src/ -type f -name 'strings.xml' | xargs sed -i 's/💜/❤️/g'

# don't revert the app name
sed -i 's/>Delta Chat</>ArcaneChat</g' ./src/main/res/values/strings.xml
