#!/usr/bin/env python3
import sys
import struct

def adjust_zip_offsets(filepath, prefix_size):
    with open(filepath, 'rb') as f:
        data = bytearray(f.read())

    print(f"Prefix size: {prefix_size} bytes")

    if data[prefix_size:prefix_size+4] != b'PK\x03\x04':
        raise ValueError(f"No ZIP signature at offset {prefix_size}")
    print("ZIP signature verified ✓")

    eocd_offset = data.rfind(b'PK\x05\x06')
    if eocd_offset == -1:
        raise ValueError("EOCD record not found")

    zip64_locator_offset = eocd_offset - 20
    is_zip64 = (zip64_locator_offset >= 0 and
                data[zip64_locator_offset:zip64_locator_offset+4] == b'PK\x06\x07')

    if is_zip64:
        print("ZIP64 format detected")

        # ZIP64 Locator: correct pointer at ZIP64 EOCD
        z64_eocd_offset_pos = zip64_locator_offset + 8
        z64_eocd_offset = struct.unpack_from('<Q', data, z64_eocd_offset_pos)[0]
        corrected_z64_eocd_offset = z64_eocd_offset + prefix_size
        struct.pack_into('<Q', data, z64_eocd_offset_pos, corrected_z64_eocd_offset)
        print(f"ZIP64 Locator: {z64_eocd_offset:#018x} -> {corrected_z64_eocd_offset:#018x}")

        if data[corrected_z64_eocd_offset:corrected_z64_eocd_offset+4] != b'PK\x06\x06':
            raise ValueError(f"ZIP64 EOCD not found at {corrected_z64_eocd_offset:#018x}")
        print("ZIP64 EOCD verified ✓")

        # ZIP64 EOCD: correct CD-Offset
        cd_offset_pos = corrected_z64_eocd_offset + 48
        cd_offset = struct.unpack_from('<Q', data, cd_offset_pos)[0]
        corrected_cd_offset = cd_offset + prefix_size
        struct.pack_into('<Q', data, cd_offset_pos, corrected_cd_offset)
        print(f"ZIP64 EOCD cd_offset: {cd_offset:#018x} -> {corrected_cd_offset:#018x}")

        # set classic EOCD to ZIP64-placeholder value
        struct.pack_into('<H', data, eocd_offset + 8,  0xFFFF)
        struct.pack_into('<H', data, eocd_offset + 10, 0xFFFF)
        struct.pack_into('<I', data, eocd_offset + 12, 0xFFFFFFFF)
        struct.pack_into('<I', data, eocd_offset + 16, 0xFFFFFFFF)
        print("Classic EOCD set to ZIP64 placeholder values ✓")

        # All Central Directory Entries: correct LFH-Offsets
        print("Adjusting Central Directory entries...")
        cd_pos = corrected_cd_offset
        entry_count = 0
        while data[cd_pos:cd_pos+4] == b'PK\x01\x02':
            filename_len = struct.unpack_from('<H', data, cd_pos + 28)[0]
            extra_len    = struct.unpack_from('<H', data, cd_pos + 30)[0]
            comment_len  = struct.unpack_from('<H', data, cd_pos + 32)[0]
            lfh_offset_pos = cd_pos + 42
            lfh_offset = struct.unpack_from('<I', data, lfh_offset_pos)[0]
            if lfh_offset != 0xFFFFFFFF:
                struct.pack_into('<I', data, lfh_offset_pos, lfh_offset + prefix_size)
            entry_count += 1
            cd_pos += 46 + filename_len + extra_len + comment_len
        print(f"Adjusted {entry_count} Central Directory entries ✓")

    else:
        print("Classic ZIP format detected")
        cd_offset_pos = eocd_offset + 16
        cd_offset = struct.unpack_from('<I', data, cd_offset_pos)[0]
        struct.pack_into('<I', data, cd_offset_pos, cd_offset + prefix_size)
        print(f"CD offset: {cd_offset:#010x} -> {cd_offset + prefix_size:#010x}")

    with open(filepath, 'wb') as f:
        f.write(data)

    print(f"Successfully adjusted all offsets (+{prefix_size} bytes)")

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} <file> <prefix_size>")
        sys.exit(1)
    adjust_zip_offsets(sys.argv[1], int(sys.argv[2]))
