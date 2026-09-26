import csv
import sys
import os


def check_duplicates(file_path):
    if not os.path.exists(file_path):
        print(f"Error: File {file_path} not found.")
        sys.exit(1)

    seen_references = set()
    duplicates = []

    try:
        with open(
            file_path, mode='r', newline='', encoding='utf-8'
        ) as csvfile:
            reader = csv.DictReader(csvfile)
            # start=2 for 1-indexed and skipping header
            for row_num, row in enumerate(reader, start=2):
                ref = row.get('processor_reference')
                if not ref:
                    continue

                if ref in seen_references:
                    duplicates.append((row_num, ref))
                else:
                    seen_references.add(ref)

        if duplicates:
            print(f"Found {len(duplicates)} duplicate processor_reference(s):")
            for row_num, ref in duplicates:
                print(f"  - Row {row_num}: {ref}")
            return True
        else:
            print("No duplicate processor_reference values found.")
            return False

    except Exception as e:
        print(f"An error occurred: {e}")
        sys.exit(1)


if __name__ == "__main__":
    target_file = os.path.join(os.path.dirname(
        __file__), 'processor_statement.csv')
    has_duplicates = check_duplicates(target_file)
    if has_duplicates:
        sys.exit(1)
    else:
        sys.exit(0)
