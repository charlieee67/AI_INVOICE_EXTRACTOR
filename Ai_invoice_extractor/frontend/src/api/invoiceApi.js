export const API_BASE_URL = '/api';

export async function uploadInvoice(file) {
  const formData = new FormData();
  formData.append('file', file);
    //here we are appending the file to the formData object with the key 'file'. This is how the backend will know which file to process.
  const response = await fetch(`${API_BASE_URL}/invoice/extract`, {
    method: 'POST',
    body: formData,
  });
  // this fetch function is used to send post request to backend with formData object as body  we write await before fetch because we want to wait for the response from the backend before proceeding to the next line of code.
  //we can only write await inside async function so we have written async before function uploadInvoice(file)
  //unlike java here we dont write type in parameters because javascript is dynamically typed language so we dont need to write type of parameter in function definition.
  //and this is async function because we are using await inside it and we want to return a promise that resolves to the JSON response from the backend.
//promise returns  completion or failure of event 
// await means wait until promise is resolved or rejected and then return the result of promise await makes sure it skips the current execution of code and continues excecuting further code even if promise is not resolved or rejected    
  if (!response.ok) {
    // this ok method is used to check if the response from the backend is successful or not. If the response is not successful, we throw an error with the message from the backend.
    let message = 'Something went wrong';
    // here we are setting a default error message in case the backend does not return a message in the response body.
    try {
      const body = await response.json();
    //   here we storing the response body receive from backend and we still using await because response.json() is also a promise that resolves to the JSON object in the response body.
      message = body.message || message;
    //   here 
    } catch {
      // the error had no JSON body, keep the default message
    //   here we are catching the error if the response body is not a valid JSON object and we are keeping the default error message in that case.
    //even though we didnt write anything in catch block it will still catch the error and continue executing the code after catch block.
    //   if we dont write catch block then the error will be thrown and the code after catch block will not be executed.
    //   if we write catch block then the error will be caught and the code after catch block will be executed.
    // so why dont we wr
    }
    const error = new Error(message);
    //here 
    error.status = response.status;
    //here 
    throw error;
    //here we are throwing the error with the message and status code from the backend. This will be caught in the catch block of the handleFilesSelected function in App.jsx.
  }
  //why file extension is .js not js
  // as comapare to java this class is similer to class like application.properties ? 
  return response.json();
}
// export const API_BASE_URL = 'http://localhost:8080/api';

export async function getInvoices() {
  const statuses = ['PENDING_REVIEW', 'CONFIRMED'];
  const responses = await Promise.all(
    statuses.map((status) =>
      fetch(`${API_BASE_URL}/invoices?status=${status}&size=50`)
    )
  );
  if (responses.some((response) => !response.ok)) {
    throw new Error('Could not load invoices');
  }
  const pages = await Promise.all(responses.map((response) => response.json()));
  return pages.flatMap((page) => page.content);
}

export async function confirmInvoice(id) {
  const response = await fetch(`${API_BASE_URL}/invoices/${id}/confirm`, {
    method: 'POST',
  });
  if (!response.ok) {
    let message = 'Could not confirm invoice';
    try {
      const body = await response.json();
      message = body.message || message;
    } catch {
      // no JSON body, keep the default message
    }
    throw new Error(message);
  }
  return response.json();
}