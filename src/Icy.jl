module Icy
using Sockets

export icy_imshow

type2number(::Val{Int8})   =0
type2number(::Val{Int16})  =1
type2number(::Val{Int32})  =2
type2number(::Val{Float32})=3
type2number(::Val{Float64})=4

function icy_imshow(a::AbstractArray{T,N}, title="Julia") where {T<:Union{Int8,Int16,Int32,Float32,Float64},N}
    N > 4 && error("Unsupported number of dimension type")
    
    N < 2 && error("Unsupported number of dimension type")
    
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

icy_imshow(a::AbstractArray{T,N}, title="Julia") where {T<:Integer,N} = icy_imshow(Int32.(a), title)
icy_imshow(a::AbstractArray{Bool,N}, title="Julia") where {N} = icy_imshow(Int8.(a), title)
icy_imshow(a::AbstractArray{T,N}, title="Julia") where {N,T<:Number} = icy_imshow(Float64.(a), title)


end # module
