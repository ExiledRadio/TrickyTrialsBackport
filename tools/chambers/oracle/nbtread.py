"""Minimal NBT and region reader (enough for chunk structure starts)."""
import gzip
import os
import struct
import zlib


def _read(b, i, t):
    if t == 1:
        return struct.unpack_from('>b', b, i)[0], i + 1
    if t == 2:
        return struct.unpack_from('>h', b, i)[0], i + 2
    if t == 3:
        return struct.unpack_from('>i', b, i)[0], i + 4
    if t == 4:
        return struct.unpack_from('>q', b, i)[0], i + 8
    if t == 5:
        return struct.unpack_from('>f', b, i)[0], i + 4
    if t == 6:
        return struct.unpack_from('>d', b, i)[0], i + 8
    if t == 7:
        n = struct.unpack_from('>i', b, i)[0]
        return bytes(b[i + 4:i + 4 + n]), i + 4 + n
    if t == 8:
        n = struct.unpack_from('>H', b, i)[0]
        return b[i + 2:i + 2 + n].decode('utf-8', 'replace'), i + 2 + n
    if t == 9:
        et = b[i]
        n = struct.unpack_from('>i', b, i + 1)[0]
        i += 5
        out = []
        for _ in range(n):
            v, i = _read(b, i, et)
            out.append(v)
        return out, i
    if t == 10:
        out = {}
        while True:
            et = b[i]
            i += 1
            if et == 0:
                return out, i
            n = struct.unpack_from('>H', b, i)[0]
            name = b[i + 2:i + 2 + n].decode('utf-8', 'replace')
            i += 2 + n
            out[name], i = _read(b, i, et)
    if t == 11:
        n = struct.unpack_from('>i', b, i)[0]
        return list(struct.unpack_from('>%di' % n, b, i + 4)), i + 4 + 4 * n
    if t == 12:
        n = struct.unpack_from('>i', b, i)[0]
        return list(struct.unpack_from('>%dq' % n, b, i + 4)), i + 4 + 8 * n
    raise ValueError('tag %d' % t)


def parse(data):
    t = data[0]
    n = struct.unpack_from('>H', data, 1)[0]
    v, _ = _read(data, 3 + n, t)
    return v


def file(path):
    with open(path, 'rb') as f:
        data = f.read()
    if data[:2] == b'\x1f\x8b':
        data = gzip.decompress(data)
    return parse(data)


def chunk(region_dir, cx, cz):
    path = os.path.join(region_dir, 'r.%d.%d.mca' % (cx >> 5, cz >> 5))
    if not os.path.exists(path):
        return None
    with open(path, 'rb') as f:
        data = f.read()
    idx = 4 * ((cx & 31) + (cz & 31) * 32)
    loc = struct.unpack_from('>I', data, idx)[0]
    off, cnt = loc >> 8, loc & 0xFF
    if off == 0:
        return None
    length = struct.unpack_from('>I', data, off * 4096)[0]
    comp = data[off * 4096 + 4]
    raw = data[off * 4096 + 5:off * 4096 + 4 + length]
    if comp == 2:
        raw = zlib.decompress(raw)
    elif comp == 1:
        raw = gzip.decompress(raw)
    return parse(raw)
