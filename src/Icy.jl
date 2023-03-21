module Icy
using Sockets

export icy_imshow

type2number(::Val{Int8})   =0
type2number(::Val{Int16})  =1
type2number(::Val{Int32})  =2
type2number(::Val{Float32})=3
type2number(::Val{Float64})=4

function icy_imshow(a::AbstractArray{T,N}, title="Julia") where {T,N}
    if N > 4
        println("Unsupported number of dimension type")
        return
    end
    if N < 2
        println("Unsupported number of dimension type")
        return
    end
    details = "$(type2number(Val((T))))x$(join(size(a),'x'))#$(title)"
    b = hton.(a)


    println("Sending Size "*details)
   @async begin 
    client = connect(10001)
    write(client, details)
    close(client)
    println("Sending data")
    client = connect(10001)
    write(client, b)
    close(client)
    end
end


end # module
