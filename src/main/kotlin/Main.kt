package org.example
import kotlinx.coroutines.delay


data class Cliente(val id: Int,val nombre:String,val email:String,val activo: Boolean)
data class Pedido(val id: Int, val clienteId: Int, val productos:List<String>, val total: Int,val estado: EstadoPedido)//Falta estado[tipo estadoPedido]
//CLASE ESTADOPEDIDO DE PRUEBA!!!


enum class EstadoPedido{
    PENDIENTE,
    CONFIRMADO,
    ENVIADO,
    ENTREGADO


}


//ResultadoOperacion: (sealed class)
sealed class ResultadoOperacion{
}


class ServicioPedidos{
    suspend fun obtenerCliente(id: Int){
        //val cliente = cliente()
        delay(2000)
        //return Cliente(id)
    }
    suspend fun calcularTotal(productos: List<String>){
        delay(2000)


    }
    suspend fun  validarInventario(productos: List<String>){
        delay(2000)


    }
    suspend fun guardarPedido(pedido: Pedido){
        delay(2000)


    }
}


//ProcesadorPedidos
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {

    //
}