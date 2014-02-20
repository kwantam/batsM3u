# Bats! M3u

Bats! M3u is an extremely simple streaming media player.

## How to use Bats! M3u

Bats! M3u does one thing: loads m3u files containing http and https
URLs, and plays them. In principle you can also have file:// URLs in the
m3u files, but relative paths are not (presently) supported.

To use Bats! M3u, download an M3u file, then open it in your file
manager of choice. When prompted, select Bats! M3u to play the file.

If authorization is required, URLs in the m3u should use the
http://username:password@host:port/ format.

The UI is entirely handled within the notification bar. Click the
notification itself to end playback, or use the buttons to seek or
pause.

Bats! M3u respects audio focus, which means it will gracefully exit if
you start another media player. It will also pause when you remove your
headphones, and while Bats! M3u is running your headset button will
play/pause the music.

## License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.
 
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

Copyright (c) 2014 Riad S. Wahby <rsw@jfet.org> <kwantam@gmail.com>
