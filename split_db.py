import sqlite3
import os

source_db = '/home/akpsaheer/AndroidStudioProjects/qamoos/app/src/main/assets/dictionary.db'
output_dir = '/home/akpsaheer/AndroidStudioProjects/qamoos/split_dictionaries'

if not os.path.exists(output_dir):
    os.makedirs(output_dir)

def split_dictionaries():
    conn = sqlite3.connect(source_db)
    cursor = conn.cursor()

    # Get all dictionary tables from dictionary_info
    cursor.execute("SELECT table_name FROM dictionary_info")
    dictionaries = [row[0] for row in cursor.fetchall() if row[0]]

    for table in dictionaries:
        print(f"Processing: {table}...")
        dest_db = os.path.join(output_dir, f"{table.lower()}.db")

        # Connect to new database
        dest_conn = sqlite3.connect(dest_db)

        # Attach the source database to the destination connection
        dest_conn.execute(f"ATTACH DATABASE '{source_db}' AS source")

        # Create table in new database
        # We need to get the schema first
        cursor.execute(f"SELECT sql FROM sqlite_master WHERE type='table' AND name='{table}'")
        schema = cursor.fetchone()[0]
        dest_conn.execute(schema)

        # Copy data
        dest_conn.execute(f"INSERT INTO {table} SELECT * FROM source.{table}")

        # Copy indexes
        cursor.execute(f"SELECT sql FROM sqlite_master WHERE type='index' AND tbl_name='{table}'")
        indexes = cursor.fetchall()
        for idx_sql in indexes:
            if idx_sql[0]:
                dest_conn.execute(idx_sql[0])

        dest_conn.commit()
        dest_conn.close()
        print(f"Finished: {dest_db}")

    conn.close()

if __name__ == "__main__":
    split_dictionaries()
