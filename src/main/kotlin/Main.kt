package org.example
import kotlinx.coroutines.*


// ---------- MODELOS DE DATOS ----------


data class Cliente(
    val id: Int,
    val nombre: String,
    val email: String,
    val activo: Boolean
) {
    fun imprimir() {
        println("-----------Cliente-----------")
        println("Id      : $id")
        println("Nombre  : $nombre")
        println("Email   : $email")
        println("Activo  : $activo")
        println("------------------------------")
    }
}


data class Pedido(
    val id: Int,
    val clienteId: Int,
    val productos: List<String>,
    val total: Double,
    val estado: EstadoPedido
)


// ---------- ESTADOS DEL PEDIDO (sealed class) ----------


sealed class EstadoPedido(val descripcion: String) {
    object Pendiente : EstadoPedido("Pendiente")
    data class Confirmado(val fechaConfirmacion: String) : EstadoPedido("Confirmado")
    data class Enviado(val numeroSeguimiento: String) : EstadoPedido("Enviado")
    data class Entregado(val fechaEntrega: String) : EstadoPedido("Entregado")
    data class Cancelado(val motivo: String) : EstadoPedido("Cancelado")
}


// ---------- RESULTADO DE OPERACIONES ----------


sealed class ResultadoOperacion {
    data class Exito(val estadoPedido: String) : ResultadoOperacion()
    data class Error(val estadoPedido: String) : ResultadoOperacion()
    object Proceso : ResultadoOperacion()
}


fun resultado(res: ResultadoOperacion) {
    when (res) {
        is ResultadoOperacion.Exito -> println(res.estadoPedido)
        is ResultadoOperacion.Error -> println(res.estadoPedido)
        ResultadoOperacion.Proceso -> println("--Procesando--")
    }
}


fun mostrarEstado(estado: EstadoPedido) {
    when (estado) {
        is EstadoPedido.Pendiente -> println("Estado: ${estado.descripcion}")
        is EstadoPedido.Confirmado -> println("Estado: ${estado.descripcion} \n*Fecha: ${estado.fechaConfirmacion}")
        is EstadoPedido.Enviado -> println("Estado: ${estado.descripcion} \n*Numero seguimiento : ${estado.numeroSeguimiento}")
        is EstadoPedido.Entregado -> println("Estado: ${estado.descripcion} \n*Fecha entrega: ${estado.fechaEntrega}")
        is EstadoPedido.Cancelado -> println("Estado: ${estado.descripcion} \n*Motivo: ${estado.motivo}")
    }
}


fun mostrarPedidoCompleto(pedido: Pedido) {
    println("----------- Detalles del Pedido -----------")
    println("Pedido ID     : ${pedido.id}")
    println("Cliente ID    : ${pedido.clienteId}")
    println("Productos     : ${pedido.productos.joinToString()}")
    println("Total         : $${pedido.total}")
    mostrarEstado(pedido.estado)
    println("-------------------------------------------")
}


// ---------- SERVICIO DE PEDIDOS (simula el backend) ----------


class ServicioPedidos {
    val clientes = mutableListOf(
        Cliente(1, "Lorena Figueroa", "lor.Fig@gmail.cl", true),
        Cliente(2, "Maria Martinez", "mp.mart@gmail.cl", true),
        Cliente(3, "Juan Lopez", "ju.Lop@gmail.cl", false)
    )


    private val pedidosGuardados = mutableListOf<Pedido>()


    fun agregarCliente(nuevoCliente: Cliente) {
        clientes.add(nuevoCliente)
    }


    suspend fun obtenerCliente(id: Int): Cliente? {
        delay(2000)
        return clientes.find { it.id == id }
    }


    suspend fun calcularTotal(productos: List<String>): Double {
        delay(2000)
        val precios = mapOf(
            "Martillo" to 12000.0,
            "Sierra" to 15000.0,
            "Destornilladores" to 49000.0,
            "Taladro" to 20000.0
        )
        return productos.sumOf { producto -> precios[producto] ?: 0.0 }
    }


    suspend fun validarInventario(productos: List<String>): Boolean {
        delay(2000)
        val sinStock = listOf("Sierra")
        return productos.none { it in sinStock }
    }


    suspend fun guardarPedido(pedido: Pedido): ResultadoOperacion {
        delay(2000)
        pedidosGuardados.add(pedido)
        return ResultadoOperacion.Exito("Pedido: ${pedido.id} \n--> Guardado Exitoso <--")
    }


    suspend fun cancelarPedido(pedidoId: Int, motivo: String) {
        delay(2000)
        val pedido = pedidosGuardados.find { it.id == pedidoId }
        if (pedido != null) {
            val pedidoCancelado = pedido.copy(estado = EstadoPedido.Cancelado(motivo))
            pedidosGuardados.remove(pedido)
            pedidosGuardados.add(pedidoCancelado)
            println("Pedido con ID $pedidoId CANCELADO EXITOSAMENTE")
            mostrarPedidoCompleto(pedidoCancelado)
        } else {
            println("No se puede cancelar. Pedido ID $pedidoId NO EXISTE.")
        }
    }


    suspend fun obtenerPedido(id: Int): Pedido? {
        delay(2000)
        return pedidosGuardados.find { it.id == id }
    }


    fun listarIdDePedidos(): List<Int> {
        return pedidosGuardados.map { it.id }
    }
}


// ---------- PROCESADOR DE PEDIDOS ----------


class ProcesadorPedidos(private val servicio: ServicioPedidos) {
    private var contadorIdPedido = 1


    suspend fun procesoPedidoCom(clienteId: Int, productos: List<String>): ResultadoOperacion {
        println("------------ INICIANDO PEDIDO ------------")


        val cliente = servicio.obtenerCliente(clienteId)?.let { clienteEncontrado ->
            if (clienteEncontrado.activo) {
                println("Cliente activo: ${clienteEncontrado.nombre}")
                clienteEncontrado
            } else {
                println("Cliente inactivo: ${clienteEncontrado.nombre}")
                return ResultadoOperacion.Error("Error: Cliente con ID $clienteId inactivo")
            }
        } ?: return ResultadoOperacion.Error("Error: Cliente con ID $clienteId no encontrado")


        val total = servicio.calcularTotal(productos)


        val pedido = Pedido(
            id = contadorIdPedido++,
            clienteId = clienteId,
            productos = productos,
            total = total,
            estado = EstadoPedido.Pendiente
        ).apply {
            println(">>>>-----------Pedido creado-----------<<<< ")
            println("ID             : $id")
            println("Cliente        : ${cliente.nombre}")
            println("Productos      : ${this.productos.joinToString()}")
            println("Total        $ : ${this.total}")
            println(">>>>-----------------------------------<<<< ")
        }


        servicio.guardarPedido(pedido)


        if (!servicio.validarInventario(productos)) {
            println("Inventario inválido: Sierra sin stock")
            servicio.cancelarPedido(pedido.id, "Producto 'Sierra' no disponible")
            return ResultadoOperacion.Error("Pedido cancelado: Producto sin stock")
        }


        progresoEstado(pedido)
        return ResultadoOperacion.Exito("Pedido ${pedido.id} procesado exitosamente")
    }


    private suspend fun progresoEstado(pedido: Pedido) {
        println(" --> Estado del Pedido ID ${pedido.id}")
        val estados = listOf(
            EstadoPedido.Pendiente,
            EstadoPedido.Confirmado("06-10-2025"),
            EstadoPedido.Enviado("SEG-${pedido.id}"),
            EstadoPedido.Entregado("07-10-2025")
        )


        estados.forEach { estado ->
            mostrarEstado(estado)
            delay(2000)
        }
    }
}


// ---------- FUNCIÓN PRINCIPAL (main) ----------


fun main() = runBlocking {
    println("============= FERREMAX =============")


    val servicio = ServicioPedidos()
    val procesPedido = ProcesadorPedidos(servicio)


    val clienteNuevo = Cliente(4, "Fernando Cabrera", "Fer.cab@gmail.com", true)


    println("Cliente sin guardar en la lista:")
    clienteNuevo.imprimir()


    servicio.agregarCliente(clienteNuevo)


    println("Cliente guardado en la lista:")
    clienteNuevo.imprimir()


    // PEDIDOS:


    val pedido1 = procesPedido.procesoPedidoCom(2,listOf("Taladro","Martillo"))
    resultado(pedido1)

    val pedido4 = procesPedido.procesoPedidoCom(1,listOf("Taladro","Martillo"))
    resultado(pedido4)

    val pedido2 = procesPedido.procesoPedidoCom(3,listOf("Taladro","Martillo"))
    // resultado(pedido2)//Muestra Error: Cliente con ID 3 inactivo

    val pedido3 = procesPedido.procesoPedidoCom(4,listOf("Taladro"))
    resultado(pedido3)
    val pedidoSinStock = procesPedido.procesoPedidoCom(2, listOf("Taladro", "Sierra"))

    resultado(pedidoSinStock)
    //val pedidoClienteNoExite = procesPedido.procesoPedidoCom(7,listOf("Taladro","Martillo"))


    //resultado(pedidoClienteNoExite)//Error: Cliente no encontrado


}

