$body = @{
    content = "测试方法calculateSum，参数a是int类型，参数b是int类型，返回值是int类型"
} | ConvertTo-Json -Depth 10

Write-Host "Sending request to parse endpoint..."
Write-Host "Request body: $body"

try {
    $response = Invoke-RestMethod -Uri "http://127.0.0.1:8000/api/text/parse" -Method Post -Body $body -ContentType "application/json; charset=utf-8"
    Write-Host "Response received successfully!"
    Write-Host "Response data:"
    $response | ConvertTo-Json -Depth 10
} catch {
    Write-Host "Error occurred: $_"
    Write-Host "Error details: $($_.Exception.Response.StatusCode.value__)"
    Write-Host "Error message: $($_.ErrorDetails.Message)"
}
