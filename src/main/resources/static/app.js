var submitBtn = document.getElementById("submit")
var wsInputTextArea = document.getElementById("ws_input")
var wsOutputTextArea = document.getElementById("ws_output")
var url = "ws://localhost:8080/ws";
var client = Stomp.client(url);

submitBtn.onclick = (evt) => {
    client.send("/example_input", {}, wsInputTextArea.value);
}

client.connect([], () => {
    console.log("Connected");
    client.subscribe("/example_output", msg => {
        console.log(msg)
        wsOutputTextArea.value = msg
    })
}, (error) => {
    console.log("Failed to connect: " + error.headers.message)
});