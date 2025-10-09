package org.example
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking


data class Cliente(val id: Int,val nombre:String,val email:String,val activo: Boolean){
    fun imprimir(){
        println("-----------Cliente-----------")
        println("Id : $id")
        println("Nombre : $nombre")
        println("Email : $email")
        println("Activo : $activo")
        println("------------------------------")

    }
}

data class Pedido(val id: Int, val clienteId: Int, val productos: List<String>, val total: Double, val estado: EstadoPedido)
//CLASE ESTADOPEDIDO DE PRUEBA!!!


sealed class EstadoPedido(val descripcion: String){
    //PENDIENTE,
    object Pendiente : EstadoPedido("Pendiente")
    //CONFIRMADO,
    data class Confirmado(val fechaConfirmacion: String) : EstadoPedido("Confirmado")
    //ENVIADO,
    data class Enviado(val numeroSeguimiento: String) : EstadoPedido("Enviado")
    //ENTREGADO
    data class Entregado(val fechaEntrega: String) : EstadoPedido("Entregado")
    //Falta agregar cancelado
    data class Cancelado(val motivo: String): EstadoPedido("Cancelado")
}


//ResultadoOperacion: (sealed class)
sealed class ResultadoOperacion{
    data class Exito(val estadoPedido: String) : ResultadoOperacion()
    data class Error(val estadoPedido: String) : ResultadoOperacion()
    object Proceso : ResultadoOperacion()
}

fun resultado(res: ResultadoOperacion){
    when(res){
        is ResultadoOperacion.Exito -> println(res.estadoPedido)
        is ResultadoOperacion.Error -> println(res.estadoPedido)
        ResultadoOperacion.Proceso -> println("--Procesando--")
    }
}
fun mostrarEstado(estado : EstadoPedido){
    when(estado){
        is EstadoPedido.Pendiente -> println("Estado: ${estado.descripcion}")
        is EstadoPedido.Confirmado -> println("Estado: ${estado.descripcion} \n*Fecha: ${estado.fechaConfirmacion}")
        is EstadoPedido.Enviado -> println("Estado: ${estado.descripcion} \n*Numero seguimiento : ${estado.numeroSeguimiento}")
        is EstadoPedido.Entregado -> println("Estado: ${estado.descripcion}\n*Fecha entrega: ${estado.fechaEntrega}")
        //agregar cancelado
        is EstadoPedido.Cancelado -> println("Estado: ${estado.descripcion}\n*Motivo: ${estado.motivo}")

    }

}
fun mostrarPedidoCompleto(pedido: Pedido){
    println("----")
    println("Pedido ID  : ${pedido.id}")
    println("Cliente ID : ${pedido.clienteId}")
    println("Productos  : ${pedido.productos.joinToString()}")
    println("Total      : ${pedido.total}")
    mostrarEstado(pedido.estado)
    println("----")

}



//cambiamos listOf a mutable para poder agregar nuevos clientes
class ServicioPedidos{
    val clientes = mutableListOf(
        Cliente(1,"Lorena Figueroa","lor.Fig@gmail.cl" ,true),
        Cliente(2,"Maria Martinez","mp.mart@gmail.cl" ,true),
        Cliente(3,"Juan Lopez","ju.Lop@gmail.cl" ,false)

    )
    fun agregarCliente(nuevoCliente: Cliente){
        clientes.add(nuevoCliente)
    }

    private val pedidosGuardados = mutableListOf<Pedido>()

    suspend fun cancelarPedido(pedidoId: Int, motivo: String){
        delay(2000)
        val pedido = pedidosGuardados.find { it.id == pedidoId }
        if (pedido != null){
            val pedidoCancelado = pedido.copy(estado = EstadoPedido.Cancelado(motivo))
            pedidosGuardados.remove(pedido)
            pedidosGuardados.add(pedidoCancelado)
            println("Pedido con Id $pedidoId CANCELADO EXITOSAMENTE")
            mostrarPedidoCompleto(pedidoCancelado)
        } else{
            println("No se puede cancelar Pedido Id $pedidoId NO EXISTE")
        }
    }

    suspend fun obtenerPedido(id: Int): Pedido?{
        delay(2000)
        return pedidosGuardados.find {it.id == id}
    }

    suspend fun obtenerCliente(id: Int): Cliente?{
        //val cliente = cliente()
        delay(2000)
        return clientes.find { it.id == id }
    }
    suspend fun calcularTotal(productos: List<String>): Double{
        delay(2000)
        val precios = mapOf(
            "Martillo" to 12000.00,
            "Sierra" to 15000.00,
            "Destornilladores" to 49000.00,
            "Taladro" to 20000.00
        )
        return productos.sumOf { producto -> precios[producto]?: 0.0 }

    }
    suspend fun validarInventario(productos: List<String>): Boolean{
        delay(2000)
        val sinStock = listOf("Sierra")
        return productos.none{it in sinStock}

    }

    suspend fun guardarPedido(pedido: Pedido): ResultadoOperacion{
        delay(2000)
        pedidosGuardados.add(pedido)
        return ResultadoOperacion.Exito("Pedido: ${pedido.id} \n--> Guardado Exitoso <--")
    }

    fun listarIdDePedidos():List<Int>{
        return pedidosGuardados.map{ it.id }
    }
}



//ProcesadorPedidos

class ProcesadorPedidos(private  val servicio: ServicioPedidos){
    private var contadorIdPedido = 1//comienza en 1

    suspend fun procesoPedidoCom(clienteId: Int,productos: List<String>): ResultadoOperacion{
        println("-----------------------------------------")
        //Let
        val cliente = servicio.obtenerCliente(clienteId)?.let { clienteEncontrado ->
            if (clienteEncontrado.activo){
                println("Cliente activo: ${clienteEncontrado.nombre}")
                clienteEncontrado

            }else{
                println("Cliente inactivo : ${clienteEncontrado.nombre}")
                return ResultadoOperacion.Error("Error: Cliente con ID ${clienteId}  inactivo")
            }
        } ?: return ResultadoOperacion.Error("Error: Cliente no encontrado")

        val total = servicio.calcularTotal(productos)
        //Apply

        val pedido = Pedido(
            id = contadorIdPedido ++,
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

        if(!servicio.validarInventario(productos)){
            println("----")
            servicio.cancelarPedido(pedido.id,"Producto Sierra no disponible")
            return ResultadoOperacion.Error("Pedido cancelado Producto Sierra sin Stock")
        }
        progresoEstado(pedido)
        return ResultadoOperacion.Exito("Pedido ${pedido.id} procesado exitosamente")


    }

    private suspend fun progresoEstado(pedido: Pedido){
        println(" -->           Estado pedido ${pedido.id} ")
        val estados = listOf(

            EstadoPedido.Pendiente,
            EstadoPedido.Confirmado("06-10-2025"),
            EstadoPedido.Enviado("SEG-${pedido.id}"),
            EstadoPedido.Entregado("07-10-2025")
        )
        println("-----------------------------------------")

        estados.forEach { estado -> mostrarEstado(estado)
            delay(2000)}
    }

}

fun main() = runBlocking{
    //
    println("FERREMAX")
    val servicio = ServicioPedidos()
    val procesPedido = ProcesadorPedidos(servicio)

    //Crear objetos y agregar CLIENTE
    val cliente1 = Cliente(4,"Fernando Cabrera","Fer.cab@gmail.com",true)
    println("Cliente sin guardar en la lista")
    //cliente1.imprimir()
    //agregar cliente a la lista
    servicio.agregarCliente(cliente1)
    println("Cliente guardado en la lista")
    cliente1.imprimir()
    //pedido por id 2 Activo = true
    val pedido1 = procesPedido.procesoPedidoCom(2,listOf("Taladro","Martillo"))
    resultado(pedido1)

    val pedido4 = procesPedido.procesoPedidoCom(1,listOf("Taladro","Martillo"))
    resultado(pedido4)

    val pedido2 = procesPedido.procesoPedidoCom(3,listOf("Taladro","Martillo"))
    //resultado(pedido2)//Muestra Error: Cliente con ID 3 inactivo

    val pedido3 = procesPedido.procesoPedidoCom(4,listOf("Taladro"))
    resultado(pedido3)
    val pedidoSinStock = procesPedido.procesoPedidoCom(2, listOf("Taladro", "Sierra"))

    resultado(pedidoSinStock)
    //val pedidoClienteNoExite = procesPedido.procesoPedidoCom(7,listOf("Taladro","Martillo"))


    //resultado(pedidoClienteNoExite)//Error: Cliente no encontrado

}