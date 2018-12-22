var j int

func add(a,b) int {
	var c int
	c = a + b
	return c
}

func main() {
  	var x int
  	var y int
  
  	y = 0
  	x = 123
 
  	for x > 0 {
   		x = x / 2
   		y = y + x
  	}
  
  	j = add(x,y)
  
  	fmt.Println(y)
  	fmt.Println()
  	fmt.Println(j)
}