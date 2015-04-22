// Copyright (C) 2015 Masahiko Higashiyama
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package jp.scaleout;

import java.lang.String;import java.nio.charset.Charset;
import java.util.NoSuchElementException;

public class StringIterator {
    private byte[] orig;
    private int offset;

    public StringIterator(String str){
        this.orig = str.getBytes(Charset.forName("UTF-8"));
        this.offset = 0;
    }

    public void next() throws NoSuchElementException {
        this.offset += 1;
    }

    public void before() {
        this.offset -= 1;
    }

    public int current() throws NoSuchElementException {
        if(this.offset == this.orig.length){
            return 0;
        }else if(this.offset > this.orig.length || this.offset < 0){
            throw new NoSuchElementException("no element");
        }
        int res = this.orig[offset];
        if(res < 0){
            res = 256 + res;
        }
        return res;
    }

    public boolean hasNext() {
        return this.offset < this.orig.length;
    }

    public boolean hasBefore() {
        return this.offset > 0;
    }
}
