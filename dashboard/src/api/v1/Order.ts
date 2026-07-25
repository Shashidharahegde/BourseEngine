import type { OrderRequest } from "../../types/OrderRequest";
import type { TradeResponse } from "../../types/TradeResponse";

export async function sendOrder(request: OrderRequest):Promise<TradeResponse[]>{
    try{
        const response = await fetch("http://localhost:7070/api/v1/placeOrder",{
            headers:{
                "content-Type": "application/json", 
            },
            method: "POST",
            body: JSON.stringify(request)
        })

        if (!response.ok){
            return await response.json().catch(() => ({}));
        }

        return await response.json() as TradeResponse[]
    }
    catch(error){
        throw new Error(`Failed to contact backend server: ${error}`);
    }
}

