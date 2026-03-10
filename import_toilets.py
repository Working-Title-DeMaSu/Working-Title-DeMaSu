#!/usr/bin/env python3
"""공중화장실 CSV 데이터를 MySQL toilets 테이블에 임포트하는 스크립트"""

import csv
import subprocess
import sys

CSV_PATH = '/Users/parkminseon/Desktop/공중화장실정보.csv'
MYSQL_CMD = ['mysql', '-u', 'jsl26', '-p1234', 'jsl26tp']
BATCH_SIZE = 500


def escape_sql(val):
    if val is None:
        return 'NULL'
    val = str(val).replace("\\", "\\\\").replace("'", "\\'")
    return f"'{val}'"


def parse_int(val, default=0):
    try:
        return int(val)
    except (ValueError, TypeError):
        return default


def parse_float(val):
    try:
        f = float(val)
        return str(f)
    except (ValueError, TypeError):
        return 'NULL'


def yn_to_int(val):
    return 1 if val.strip().upper() == 'Y' else 0


def process_row(row):
    name = row[4].strip()
    if not name:
        return None

    address = row[5].strip() if row[5].strip() else row[6].strip()
    lat = parse_float(row[21])
    lng = parse_float(row[22])

    if lat == 'NULL' or lng == 'NULL':
        return None

    # 한국 범위 벗어나는 이상 좌표 필터링 (위도 33~39, 경도 124~132)
    lat_f = float(lat)
    lng_f = float(lng)
    if lat_f < 33 or lat_f > 39 or lng_f < 124 or lng_f > 132:
        return None

    open_hours_type = row[18].strip()
    open_hours_detail = row[19].strip()
    open_hours = open_hours_detail if open_hours_detail else open_hours_type
    is_24hours = 1 if open_hours_type == '상시' else 0

    male_disabled_toilet = parse_int(row[9])
    male_disabled_urinal = parse_int(row[10])
    female_disabled_toilet = parse_int(row[14])
    is_wheelchair = 1 if (male_disabled_toilet + male_disabled_urinal + female_disabled_toilet) > 0 else 0

    has_emergency = yn_to_int(row[26]) if len(row) > 26 else 0
    has_cctv = yn_to_int(row[28]) if len(row) > 28 else 0
    has_diaper = yn_to_int(row[29]) if len(row) > 29 else 0

    phone = row[17].strip() if row[17].strip() else None
    male_toilet_count = parse_int(row[7])
    male_urinal_count = parse_int(row[8])
    female_toilet_count = parse_int(row[13])

    return (
        f"({escape_sql(name)}, {escape_sql(address)}, {lat}, {lng}, "
        f"{escape_sql(open_hours)}, {is_24hours}, {is_wheelchair}, "
        f"0, 0, 0, {has_diaper}, 'WESTERN', {has_emergency}, "
        f"{escape_sql(phone)}, {has_cctv}, "
        f"{male_toilet_count}, {male_urinal_count}, {female_toilet_count}, "
        f"'PUBLIC_API', 'APPROVED')"
    )


def main():
    print("CSV 파일 읽는 중...")
    rows = []
    skipped = 0

    with open(CSV_PATH, 'r', encoding='cp949') as f:
        reader = csv.reader(f)
        next(reader)  # 헤더 스킵
        for row in reader:
            result = process_row(row)
            if result:
                rows.append(result)
            else:
                skipped += 1

    total = len(rows)
    print(f"총 {total}건 임포트 예정 (스킵: {skipped}건)")

    insert_prefix = (
        "INSERT INTO toilets "
        "(name, address, latitude, longitude, open_hours, "
        "is_24hours, is_wheelchair, has_paper, has_soap, has_sanitary, "
        "has_diaper, toilet_type, has_emergency, phone, has_cctv, "
        "male_toilet_count, male_urinal_count, female_toilet_count, "
        "source, status) VALUES "
    )

    imported = 0
    for i in range(0, total, BATCH_SIZE):
        batch = rows[i:i + BATCH_SIZE]
        sql = insert_prefix + ",\n".join(batch) + ";"

        result = subprocess.run(
            MYSQL_CMD,
            input=sql,
            capture_output=True,
            text=True
        )

        if result.returncode != 0:
            print(f"ERROR at batch {i}: {result.stderr}")
            sys.exit(1)

        imported += len(batch)
        pct = int(imported / total * 100)
        print(f"  [{pct:3d}%] {imported}/{total} 건 완료")

    print(f"\n임포트 완료! 총 {imported}건 저장됨")


if __name__ == '__main__':
    main()
