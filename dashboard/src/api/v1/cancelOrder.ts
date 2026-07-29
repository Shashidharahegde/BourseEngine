export async function cancelOrder(
  symbol: string,
  orderId: string
): Promise<void> {
  try {
    const response = await fetch(
      `http://localhost:7070/api/v1/orders/${symbol}/${orderId}`,
      {
        method: "DELETE",
      }
    );

    if (!response.ok) {
      throw new Error(await response.text());
    }
  } catch (error) {
    throw new Error(`Failed to contact backend server: ${error}`);
  }
}