#!/bin/sh

find ./src/main/assets/help/ -type f -name '*.html' | xargs sed -i 's/get.delta.chat/github.com\/ArcaneChat/g'
find ./src/main/assets/help/ -type f -name '*.html' | xargs sed -i 's/Delta Chat/ArcaneChat/g'

find ./src/ -type f -name 'strings.xml' | xargs sed -i 's/get.delta.chat/github.com\/ArcaneChat/g'
find ./src/ -type f -name 'strings.xml' | xargs sed -i 's/delta.chat\/donate/arcanechat.me\/#contribute/g'
find ./src/ -type f -name 'strings.xml' | xargs sed -i 's/Delta Chat/ArcaneChat/g'
find ./src/ -type f -name 'strings.xml' | xargs sed -i 's/❤️/💜/g'
