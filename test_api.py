#!/usr/bin/env python3
"""
Script para ejecutar pruebas HTTP contra la API SICC
"""

import requests
import json
import sys
from typing import Dict, Any

# Configuración
HOST = "localhost"
PORT = "8080"
BASE_URL = f"http://{HOST}:{PORT}"

# Colores para output
GREEN = '\033[92m'
RED = '\033[91m'
YELLOW = '\033[93m'
BLUE = '\033[94m'
RESET = '\033[0m'

def print_test(name: str):
    print(f"\n{BLUE}{'='*60}")
    print(f"TEST: {name}")
    print(f"{'='*60}{RESET}")

def print_success(msg: str):
    print(f"{GREEN}✓ {msg}{RESET}")

def print_error(msg: str):
    print(f"{RED}✗ {msg}{RESET}")

def print_info(msg: str):
    print(f"{YELLOW}→ {msg}{RESET}")

def check_server():
    """Verificar si el servidor está disponible"""
    print_test("Verificar disponibilidad del servidor")
    try:
        response = requests.get(f"{BASE_URL}/actuator/health", timeout=2)
        if response.status_code == 200:
            print_success(f"Servidor disponible en {BASE_URL}")
            return True
        else:
            print_error(f"Servidor respondió con status {response.status_code}")
            return False
    except Exception as e:
        print_error(f"No se pudo conectar al servidor: {e}")
        print_info("Asegúrate de que el servidor está corriendo:")
        print_info("  cd C:\\Users\\npach\\IdeaProjects\\sicc\\sicc-api")
        print_info("  mvnw.cmd spring-boot:run")
        return False

def test_register():
    """Test: Registro de usuario"""
    print_test("Registro de usuario")

    payload = {
        "firstname": "Juan",
        "lastname": "Pérez",
        "email": f"juan_{int(__import__('time').time())}@example.com",
        "password": "password123"
    }

    try:
        response = requests.post(
            f"{BASE_URL}/api/auth/register",
            json=payload,
            timeout=5
        )

        print_info(f"POST /api/auth/register")
        print_info(f"Status: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print_success(f"Registro exitoso")
            print(f"  Email: {data.get('email')}")
            print(f"  Token: {data.get('token')[:50]}...")
            return data.get('token'), data.get('email')
        else:
            print_error(f"Error en registro: {response.text}")
            return None, payload['email']
    except Exception as e:
        print_error(f"Excepción: {e}")
        return None, payload['email']

def test_login(email: str, password: str = "password123"):
    """Test: Login"""
    print_test("Login de usuario")

    payload = {
        "email": email,
        "password": password
    }

    try:
        response = requests.post(
            f"{BASE_URL}/api/auth/login",
            json=payload,
            timeout=5
        )

        print_info(f"POST /api/auth/login")
        print_info(f"Status: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print_success(f"Login exitoso")
            print(f"  Email: {data.get('email')}")
            print(f"  Token: {data.get('token')[:50]}...")
            return data.get('token')
        else:
            print_error(f"Error en login: {response.text}")
            return None
    except Exception as e:
        print_error(f"Excepción: {e}")
        return None

def test_get_current_user(token: str):
    """Test: Obtener usuario actual"""
    print_test("Obtener usuario actual")

    headers = {"Authorization": f"Bearer {token}"}

    try:
        response = requests.get(
            f"{BASE_URL}/api/users/me",
            headers=headers,
            timeout=5
        )

        print_info(f"GET /api/users/me")
        print_info(f"Status: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print_success(f"Usuario obtenido correctamente")
            print(f"  ID: {data.get('id')}")
            print(f"  Email: {data.get('email')}")
            print(f"  Nombre: {data.get('firstname')} {data.get('lastname')}")
            print(f"  Rol: {data.get('role')}")
            return True
        else:
            print_error(f"Error: {response.text}")
            return False
    except Exception as e:
        print_error(f"Excepción: {e}")
        return False

def test_consultations(token: str):
    """Test: Obtener consultas"""
    print_test("Obtener consultas")

    headers = {"Authorization": f"Bearer {token}"}

    try:
        response = requests.get(
            f"{BASE_URL}/api/consultations?page=0&size=10",
            headers=headers,
            timeout=5
        )

        print_info(f"GET /api/consultations?page=0&size=10")
        print_info(f"Status: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print_success(f"Consultas obtenidas correctamente")
            if isinstance(data, dict) and 'content' in data:
                print(f"  Total: {data.get('totalElements', 'N/A')}")
                print(f"  Items: {len(data.get('content', []))}")
            else:
                print(f"  Items: {len(data) if isinstance(data, list) else 'N/A'}")
            return True
        else:
            print_error(f"Error: {response.text}")
            return False
    except Exception as e:
        print_error(f"Excepción: {e}")
        return False

def test_patients(token: str):
    """Test: Obtener pacientes"""
    print_test("Obtener pacientes")

    headers = {"Authorization": f"Bearer {token}"}

    try:
        response = requests.get(
            f"{BASE_URL}/api/patients?page=0&size=10",
            headers=headers,
            timeout=5
        )

        print_info(f"GET /api/patients?page=0&size=10")
        print_info(f"Status: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print_success(f"Pacientes obtenidos correctamente")
            if isinstance(data, dict) and 'content' in data:
                print(f"  Total: {data.get('totalElements', 'N/A')}")
                print(f"  Items: {len(data.get('content', []))}")
            else:
                print(f"  Items: {len(data) if isinstance(data, list) else 'N/A'}")
            return True
        else:
            print_error(f"Error: {response.text}")
            return False
    except Exception as e:
        print_error(f"Excepción: {e}")
        return False

def test_prescriptions(token: str):
    """Test: Obtener prescripciones"""
    print_test("Obtener prescripciones")

    headers = {"Authorization": f"Bearer {token}"}

    try:
        response = requests.get(
            f"{BASE_URL}/api/prescriptions?page=0&size=10",
            headers=headers,
            timeout=5
        )

        print_info(f"GET /api/prescriptions?page=0&size=10")
        print_info(f"Status: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print_success(f"Prescripciones obtenidas correctamente")
            if isinstance(data, dict) and 'content' in data:
                print(f"  Total: {data.get('totalElements', 'N/A')}")
                print(f"  Items: {len(data.get('content', []))}")
            else:
                print(f"  Items: {len(data) if isinstance(data, list) else 'N/A'}")
            return True
        else:
            print_error(f"Error: {response.text}")
            return False
    except Exception as e:
        print_error(f"Excepción: {e}")
        return False

def test_professionals(token: str):
    """Test: Obtener profesionales"""
    print_test("Obtener profesionales")

    headers = {"Authorization": f"Bearer {token}"}

    try:
        response = requests.get(
            f"{BASE_URL}/api/healthcareprofessionals?page=0&size=10",
            headers=headers,
            timeout=5
        )

        print_info(f"GET /api/healthcareprofessionals?page=0&size=10")
        print_info(f"Status: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print_success(f"Profesionales obtenidos correctamente")
            if isinstance(data, dict) and 'content' in data:
                print(f"  Total: {data.get('totalElements', 'N/A')}")
                print(f"  Items: {len(data.get('content', []))}")
            else:
                print(f"  Items: {len(data) if isinstance(data, list) else 'N/A'}")
            return True
        else:
            print_error(f"Error: {response.text}")
            return False
    except Exception as e:
        print_error(f"Excepción: {e}")
        return False

def test_invalid_token():
    """Test: Token inválido"""
    print_test("Prueba de token inválido")

    headers = {"Authorization": "Bearer invalid_token"}

    try:
        response = requests.get(
            f"{BASE_URL}/api/patients",
            headers=headers,
            timeout=5
        )

        print_info(f"GET /api/patients (sin token válido)")
        print_info(f"Status: {response.status_code}")

        if response.status_code >= 401:
            print_success(f"Correctamente rechazado (status {response.status_code})")
            return True
        else:
            print_error(f"Debería rechazarse pero retornó {response.status_code}")
            return False
    except Exception as e:
        print_error(f"Excepción: {e}")
        return False

def test_no_token():
    """Test: Sin token"""
    print_test("Prueba sin token")

    try:
        response = requests.get(
            f"{BASE_URL}/api/patients",
            timeout=5
        )

        print_info(f"GET /api/patients (sin token)")
        print_info(f"Status: {response.status_code}")

        if response.status_code >= 401:
            print_success(f"Correctamente rechazado (status {response.status_code})")
            return True
        else:
            print_error(f"Debería rechazarse pero retornó {response.status_code}")
            return False
    except Exception as e:
        print_error(f"Excepción: {e}")
        return False

def main():
    print(f"\n{BLUE}{'='*60}")
    print("SUITE DE PRUEBAS HTTP - SICC API")
    print(f"{'='*60}{RESET}\n")

    # Verificar servidor
    if not check_server():
        sys.exit(1)

    # Pruebas de seguridad
    print("\n" + "="*60)
    print("PRUEBAS DE SEGURIDAD")
    print("="*60)
    test_invalid_token()
    test_no_token()

    # Prueba de autenticación
    print("\n" + "="*60)
    print("PRUEBAS DE AUTENTICACIÓN")
    print("="*60)

    token, email = test_register()
    if not token:
        print_error("No se pudo obtener token del registro")
        sys.exit(1)

    # Pruebas con token válido
    print("\n" + "="*60)
    print("PRUEBAS CON TOKEN VÁLIDO")
    print("="*60)

    test_get_current_user(token)
    test_patients(token)
    test_consultations(token)
    test_prescriptions(token)
    test_professionals(token)

    # Resumen
    print(f"\n{BLUE}{'='*60}")
    print("PRUEBAS COMPLETADAS")
    print(f"{'='*60}{RESET}\n")

if __name__ == "__main__":
    main()

