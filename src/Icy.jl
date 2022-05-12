module Icy
using Sockets

export icy_imshow

function createHeader(a, title)
    details = ""
    typeA = eltype(a)
    if typeA == Int32
        details = details*"0"
    elseif typeA == Int64
        details = details*"1"
    elseif typeA == Float32
        details = details*"2"
    elseif typeA == Float64
        details = details*"3"
    else
        println("Invalid data type")
        return ""
    end

    for i in size(a)
       details *= "x"*string(i)
    end
    details *= "#"*title
    return details
end

function icy_imshow(a, title="Julia")
    details = createHeader(a, title)
    if details == ""
        println("Please convert the matrix in a supported type")
        return
    end
    b = similar(a)
    for i in 1:length(a)
       b[i] = hton(a[i])
    end
    
    println("Sending Size "*details)
    client = connect(10001)
    write(client, details)
    close(client)
    println("Sending data")
    client = connect(10001)
    write(client, b)
    close(client)
end


end # module
