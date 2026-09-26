from datetime import datetime

t1 = datetime.strptime("02:30:49.27", "%H:%M:%S.%f")
t2 = datetime.strptime("02:30:47.63", "%H:%M:%S.%f")

diff = t1 - t2
print(f"Diff: {diff}")
