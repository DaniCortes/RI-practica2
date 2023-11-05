import flask
import mysql.connector
import json
from flask import jsonify, Response, request
from mysql.connector import errorcode
from flask_cors import CORS

app = flask.Flask(__name__)
CORS(app)
app.config["DEBUG"] = True
app.config['JSON_AS_ASCII'] = False


def get_cnx():
    return mysql.connector.connect(user='sie', password='sie',
                                   host='localhost', port=3360,
                                   database='SIE',
                                   )


@app.route('/', methods=['GET'])
def index():
    return {"message": "Hello, world!"}, 200


@app.route('/employees', methods=['GET'])
def get_employees():
    cnx = get_cnx()
    cursor = cnx.cursor(dictionary=True)
    get_employees = "SELECT dni, name, surname, " + \
        "DATE_FORMAT(bdate, '%Y-%m-%d') as bdate," + \
        "salary FROM employee"
    cursor.execute(get_employees)
    employees_data = cursor.fetchall()
    cursor.close()
    cnx.close()

    if not employees_data:
        return Response(status=204)

    return {"employees": employees_data}, 200


@app.route('/employee/<dni>', methods=['GET'])
def get_employee(dni):
    employee_dni = {'dni': dni}

    cnx = get_cnx()
    cursor = cnx.cursor(dictionary=True)
    get_employee = "SELECT dni, name, surname, " + \
        "DATE_FORMAT(bdate, '%Y-%m-%d') as bdate," + \
        "salary FROM employee WHERE dni = %(dni)s"
    cursor.execute(get_employee, employee_dni)
    employee_data = cursor.fetchone()
    cursor.close()
    cnx.close()

    if not employee_data:
        return Response(status=204)

    return {"employee": employee_data}, 200


@app.route('/employee', methods=['POST'])
def add_employee():
    employee_data = {'dni': request.json['dni'],
                     'name': request.json['name'],
                     'surname': request.json['surname'],
                     'bdate': request.json['bdate'],
                     'salary': request.json['salary']}

    cnx = get_cnx()
    cursor = cnx.cursor()
    add_employee = "INSERT INTO employee(dni, name, surname, bdate, salary) " \
                   "VALUES (%(dni)s, %(name)s, %(surname)s, %(bdate)s, " \
                   "%(salary)s)"

    try:
        cursor.execute(add_employee, employee_data)
        cnx.commit()
        cursor.close()
        cnx.close()
    except mysql.connector.Error as err:
        if err.errno == errorcode.ER_DUP_ENTRY:
            return {"error": "An employee with this ID already "
                    "exists."}, 400

        elif err.errno == 4025:
            return {"error": "Salary must be greater than or equal to 0."}, 400

        return {"error": err.msg}, 400

    return {"success": "Employee added successfully."}, 201


@app.route('/employee/<dni>', methods=['PUT', 'PATCH'])
def updateEmployee(dni):
    employee_dni = {'dni': dni}

    cnx = get_cnx()
    cursor = cnx.cursor(dictionary=True)
    get_employee_data = "SELECT * FROM employee WHERE dni = %(dni)s"

    cursor.execute(get_employee_data, employee_dni)
    employee_data = cursor.fetchone()

    if not employee_data:
        return {"error": "Could not update the employee because they "
                "did not exist anyway."}, 418

    if request.json['name'] is not None:
        employee_data['name'] = request.json['name']
    if request.json['surname'] is not None:
        employee_data['surname'] = request.json['surname']
    if request.json['bdate'] is not None:
        employee_data['bdate'] = request.json['bdate']
    if request.json['salary'] is not None:
        employee_data['salary'] = request.json['salary']

    update_employee = "UPDATE employee SET name = %(name)s," \
        " surname = %(surname)s, bdate = %(bdate)s," \
        " salary = %(salary)s WHERE dni = %(dni)s"

    try:
        cursor.execute(update_employee, employee_data)
        rowcount = cursor.rowcount
        cnx.commit()
        cursor.close()
        cnx.close()
    except mysql.connector.Error as err:
        if err.errno == 4025:
            return {"error": "Salary must be greater than or equal to 0."}, 409
        return {"error": err.msg}, 400

    if not rowcount:
        return {"error": "Could not update the employee because the "
                "modified values are the same as the "
                "original values."}, 418

    return {"success": "Employee updated successfully."}, 200


@app.route('/employee/<dni>', methods=['DELETE'])
def deleteEmployee(dni):
    employee_data = {'dni': dni}

    cnx = get_cnx()
    cursor = cnx.cursor()
    delete_employee = "DELETE FROM employee WHERE employee.dni = %(dni)s"

    try:
        cursor.execute(delete_employee, employee_data)
        rowcount = cursor.rowcount
        cnx.commit()
        cursor.close()
        cnx.close()
    except mysql.connector.Error as err:
        if err.errno == errorcode.ER_ROW_IS_REFERENCED_2:
            return {"error": "Could not delete the employee because there is "
                    "at least one customer bound to them."}, 409
        return {"error": err.msg}, 400

    if not rowcount:
        return {"error": "Could not delete the employee because they did not "
                "exist anyway."}, 418

    return {"success": "Employee deleted successfully."}, 200


@app.route('/clients', methods=['GET'])
def get_clients():
    cnx = get_cnx()
    cursor = cnx.cursor(dictionary=True)
    get_clients = "SELECT dni, name, surname, " + \
        "DATE_FORMAT(bdate, '%Y-%m-%d') as bdate," + \
        "manager FROM client"
    cursor.execute(get_clients)
    clients_data = cursor.fetchall()
    cursor.close()
    cnx.close()

    if not clients_data:
        return Response(status=204)

    return {"clients": clients_data}, 200


@app.route('/client/<dni>', methods=['GET'])
def get_client(dni):
    client_dni = {'dni': dni}

    cnx = get_cnx()
    cursor = cnx.cursor(dictionary=True)
    get_client = "SELECT dni, name, surname, " + \
        "DATE_FORMAT(bdate, '%Y-%m-%d') as bdate," + \
        "manager FROM client WHERE dni = %(dni)s"
    cursor.execute(get_client, client_dni)
    client_data = cursor.fetchone()
    cursor.close()
    cnx.close()

    if not client_data:
        return Response(status=204)

    return {"client": client_data}, 200


@app.route('/client', methods=['POST'])
def add_client():
    client_data = {'dni': request.json['dni'],
                   'name': request.json['name'],
                   'surname': request.json['surname'],
                   'bdate': request.json['bdate'],
                   'manager': request.json['manager']}

    if client_data['dni'] == client_data['manager']:
        return {"error": "The client's manager cannot be the client "
                "themselves."}, 409

    cnx = get_cnx()
    cursor = cnx.cursor()

    add_client = "INSERT INTO client(dni, name, surname, bdate, manager) " \
                 "VALUES (%(dni)s, %(name)s, %(surname)s, %(bdate)s, " \
                 "%(manager)s)"

    try:
        cursor.execute(add_client, client_data)
        cnx.commit()
        cursor.close()
        cnx.close()
    except mysql.connector.Error as err:
        if err.errno == errorcode.ER_DUP_ENTRY:
            return {"error": "An client with this ID already exists"}, 400

        elif err.errno == errorcode.ER_NO_REFERENCED_ROW:
            return {"error": "Could not add the client because the referenced "
                    "manager does not exist."}, 400

        return {"error": err.msg}, 400

    return {"success": "Client added successfully."}, 201


@app.route('/client/<dni>', methods=['PUT', 'PATCH'])
def update_client(dni):
    client_dni = {'dni': dni}
    if request.json['manager'] is not None:
        if client_data['dni'] == request.json['manager']:
            return {"error": "The client's manager cannot be the client "
                    "themselves."}, 409

    cnx = get_cnx()
    cursor = cnx.cursor(dictionary=True)
    get_client_data = "SELECT * FROM client WHERE dni = %(dni)s"

    cursor.execute(get_client_data, client_dni)
    client_data = cursor.fetchone()

    if not client_data:
        return {"error": "Could not update the client because they did not "
                "exist anyway."}, 418

    if request.json['name'] is not None:
        client_data['name'] = request.json['name']
    if request.json['surname'] is not None:
        client_data['surname'] = request.json['surname']
    if request.json['bdate'] is not None:
        client_data['bdate'] = request.json['bdate']
    if request.json['manager'] is not None:
        client_data['manager'] = request.json['manager']

    update_client = "UPDATE client SET name = %(name)s," \
        " surname = %(surname)s, bdate = %(bdate)s," \
        " manager = %(manager)s WHERE dni = %(dni)s"

    try:
        cursor.execute(update_client, client_data)
        rowcount = cursor.rowcount
        cnx.commit()
        cursor.close()
        cnx.close()
    except mysql.connector.Error as err:
        if err.errno == errorcode.ER_NO_REFERENCED_ROW:
            return {"error": "Could not update the client because the "
                    "referenced manager does not exist."}, 400

        return {"error": err.msg}, 400

    if not rowcount:
        return {"error": "Could not update the client because the modified "
                "values are the same as the original values."}, 418

    return {"success": "Client's manager updated successfully."}, 200


@app.route('/client/<dni>', methods=['DELETE'])
def delete_client(dni):
    client_data = {'dni': dni}

    cnx = get_cnx()
    cursor = cnx.cursor()
    delete_client = "DELETE FROM client WHERE client.dni = %(dni)s"

    try:
        cursor.execute(delete_client, client_data)
        rowcount = cursor.rowcount
        cnx.commit()
        cursor.close()
        cnx.close()
    except mysql.connector.Error as err:
        return {"error": err.msg}, 400

    if not rowcount:
        return {"error": "Could not delete the client because they did not "
                "exist anyway."}, 418

    return {"success": "Client deleted successfully."}, 200


if __name__ == "__main__":
    app.run()
