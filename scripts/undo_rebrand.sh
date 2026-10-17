#!/bin/sh

find ./src/main/assets/help/ -type f -name '*.html' | xargs sed -i 's|arcanechat.me/privacy|delta.chat/privacy|g'
find ./src/main/assets/help/ -type f -name '*.html' | xargs sed -i 's|arcanechat.me/#download|get.delta.chat|g'

find ./src/main/assets/help/fa -type f -name '*.html' | xargs sed -i 's|«ArcaneChat»|«Delta Chat»|g'
find ./src/main/assets/help/fa -type f -name '*.html' | xargs sed -i 's|of ArcaneChat developers|of Delta Chat developers|g'
find ./src/main/assets/help/fa -type f -name '*.html' | xargs sed -i 's|Older ArcaneChat versions|Older Delta Chat versions|g'
find ./src/main/assets/help/fa -type f -name '*.html' | xargs sed -i 's|your ArcaneChat contact|your Delta Chat contact|g'
find ./src/main/assets/help/fa -type f -name '*.html' | xargs sed -i 's|/ ArcaneChat»|/ Delta Chat»|g'
find ./src/main/assets/help/fa -type f -name '*.html' | xargs sed -i 's|>ArcaneChat V2<|>Delta Chat V2<|g'
find ./src/main/assets/help/fa -type f -name '*.html' | xargs sed -i 's|/ ArcaneChat /|/ Delta Chat /|g'
find ./src/main/assets/help/fa -type f -name '*.html' | xargs sed -i 's|ArcaneChat|دلتاچت|g'

find ./src/main/assets/help/ -type f -name '*.html' | xargs sed -i 's|ArcaneChat|Delta Chat|g'

find ./src/ -type f -name 'strings.xml' | xargs sed -i 's|arcanechat.me/#download|get.delta.chat|g'
find ./src/ -type f -name 'strings.xml' | xargs sed -i 's|arcanechat.me/#contribute|delta.chat/donate|g'

find ./src/main/res/values-fa -type f -name 'strings.xml' | xargs sed -i 's|ArcaneChat|دلتا چت|g'
find ./src/main/res/values-bqi -type f -name 'strings.xml' | xargs sed -i 's|ArcaneChat|دلتا چت|g'
find ./src/ -type f -name 'strings.xml' | xargs sed -i 's|ArcaneChat-Version|Delta-Chat-Version|g'
find ./src/ -type f -name 'strings.xml' | xargs sed -i 's|ArcaneChat-Installation|Delta-Chat-Installation|g'

find ./src/ -type f -name 'strings.xml' | xargs sed -i 's|ArcaneChat|Delta Chat|g'
find ./src/ -type f -name 'strings.xml' | xargs sed -i 's|💜|❤️|g'

# don't revert the app name
sed -i 's|>Delta Chat<|>ArcaneChat<|g' ./src/main/res/values/strings.xml
