"""Seed fictional README records in a fresh localhost:8081 demo, never a live vault."""
import argparse
import calendar
import datetime as dt
import http.cookiejar
import json
import os
import urllib.parse
import urllib.request

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--confirm-demo-database', action='store_true', required=True)
parser.parse_args()
password = os.environ.get('DEMO_ADMIN_PASSWORD')
if not password:
    raise SystemExit('Set DEMO_ADMIN_PASSWORD for the isolated demo instance.')
base = 'http://127.0.0.1:8081/api'
client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))


def request(path, method='GET', value=None, token=None):
    headers = {}
    data = None
    if value is not None:
        headers['Content-Type'] = 'application/json'
        data = json.dumps(value).encode()
    if token:
        headers[token['headerName']] = token['token']
    with client.open(urllib.request.Request(base + path, data=data, headers=headers, method=method)) as response:
        body = response.read()
        return json.loads(body) if body else None


token = request('/auth/csrf')
login = urllib.request.Request(base + '/auth/login', method='POST', data=urllib.parse.urlencode({'username': 'admin', 'password': password}).encode(), headers={'Content-Type': 'application/x-www-form-urlencoded', token['headerName']: token['token']})
client.open(login).close()
token = request('/auth/csrf')
if request('/expenses')['totalElements'] != 0:
    raise SystemExit('Refusing to seed a non-empty database. Use a fresh demo only.')
categories = {item['name']: item['id'] for item in request('/categories')}
today = dt.date.today()


def months(date, offset):
    number = date.year * 12 + date.month - 1 + offset
    year, month = divmod(number, 12)
    month += 1
    return dt.date(year, month, min(date.day, calendar.monthrange(year, month)[1]))


items = [
    ('Home internet', 'Utilities', 'BILL', '79.99', 2, False),
    ('StreamSpace', 'Entertainment', 'SUBSCRIPTION', '15.99', 4, False),
    ('Studio tools', 'Software', 'SUBSCRIPTION', '29.00', 7, False),
    ('Apartment rent', 'Housing', 'BILL', '1250.00', 20, False),
    ('Electricity', 'Utilities', 'BILL', '94.00', -3, True),
    ('Music Garden', 'Entertainment', 'SUBSCRIPTION', '10.99', 10, False),
    ('Home insurance', 'Insurance', 'BILL', '42.50', 13, False),
    ('Cloud notes', 'Software', 'SUBSCRIPTION', '8.00', 18, False),
]
for index, (name, category, kind, amount, offset, variable) in enumerate(items):
    due = today + dt.timedelta(days=offset)
    start = months(due, -5)
    value = {'name': name, 'description': 'Fictional example for README screenshots.', 'kind': kind, 'categoryId': categories[category], 'amount': amount, 'currency': 'USD', 'frequency': 'MONTHLY', 'customInterval': None, 'customUnit': None, 'startDate': start.isoformat(), 'nextDueDate': start.isoformat(), 'endDate': None, 'paymentMethod': 'Demo card', 'websiteUrl': '', 'status': 'ACTIVE', 'variableAmount': variable, 'notes': 'Fictional demo data; not a real financial record.'}
    expense = request('/expenses', 'POST', value, token)
    for occurrence in range(5):
        date = dt.date.fromisoformat(expense['nextDueDate'])
        request('/expenses/' + str(expense['id']) + '/payments', 'POST', {'occurrenceDate': date.isoformat(), 'paymentDate': date.isoformat(), 'amount': amount, 'notes': 'Fictional historical demo payment.'}, token)
        expense = request('/expenses/' + str(expense['id']))
    if index in (5, 7):
        request('/expenses/' + str(expense['id']) + '/payments', 'POST', {'occurrenceDate': expense['nextDueDate'], 'paymentDate': today.isoformat(), 'amount': amount, 'notes': 'Fictional early payment for the demo.'}, token)
request('/notifications/refresh', 'POST', token=token)
print('Created 8 fictional demo expenses with payment history on localhost:8081 only.')
