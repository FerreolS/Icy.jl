# JuliaIcy
Julia code to display images in [Icy](http://icy.bioimageanalysis.org/)

# Goal
In Julia there are many frameworks to display images, so why not adding another ? 

The idea is to be able to use all the tools from [Icy](http://icy.bioimageanalysis.org/) and its nice GUI.

# HowTo
* In Icy

open icyJulia plugin

![Alt text](tutoIcyJulia1.jpg?raw=true "Choosing")

Then launch it

![Alt text](tutoIcyJulia2.jpg?raw=true "Opened")

Start the plugin

![Alt text](tutoIcyJulia3.jpg?raw=true "Running")

Now you can switch to Julia:

* Julia
``` julia
include("icyJulia.jl")
icy_imshow(data)
icy_imshow(data, "ImageName")
```

# Note

Because of java limitations, only `Int32, Int64, Float32, Float64` types are supported, so please convert your data before using `icy_imshow`.
