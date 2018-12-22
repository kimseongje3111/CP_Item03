var a[10] int
var b,c int

func two() (int, int) {
	return 1, 2
}

func main() {
	var a int
	var x[10] int
	var b,c int = 1,2
	var d int = 3
		
	x[5] = 10
	a = x[5]
	
	if a != 10 {
		++ a
	} else {
		a = 0
	}
	
	fmt.Println(a)
	fmt.Scanln(b)
}
