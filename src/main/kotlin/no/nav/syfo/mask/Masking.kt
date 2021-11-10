package no.nav.syfo.mask

fun String.maskLast(count: Int): String {
    var x = this.length - count
    if (x < 0) {
        return this
    } else if (x > this.length) {
        x = this.length
    }

    return this.substring(x, x + count).padStart(this.length, 'x')
}

