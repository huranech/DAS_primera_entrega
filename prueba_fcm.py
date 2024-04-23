import requests

'''
[ INSTRUCCIONES ]
1. Instalar la libreria requests con: pip install requests
2. Modificar los valores de la variable datos_receta señalados entre corchetes
3. Ejecutar python prueba_fcm.py en el contexto del archivo (es importante tener python instalado)
Se debería mostrar una notificación en la aplicación de Android.
'''

# URL y ruta del servidor web
url = 'http://34.65.250.38:8080/notificar_dispositivos.php'

'''
[ HARDCODE ] cambiar nombre, ingredientes y descripción para hacer las pruebas
'''
datos_receta = {
    'notificar_dispositivos': True,
    'nombre': '[ Nombre de la receta ]',
    'ingredientes': '[ Lista de ingredientes ]',
    'descripcion': '[ Descripción de la receta ]'
}

# Realizar la solicitud POST
try:
    response = requests.post(url, data=datos_receta)
    if response.status_code == 200:
        print("Solicitud enviada correctamente")
        print("Respuesta del servidor:")
        print(response.text)
    else:
        print("Error en la solicitud: ", response.status_code)
except requests.exceptions.RequestException as e:
    print("Error al enviar la solicitud:", e)