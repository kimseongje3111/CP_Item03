
public class ASTNode {

	private String Id;
	private String Num;
	private boolean isArray;
	private boolean isParam;

	public ASTNode(String id, String num, boolean isArray) {
		this.Id = id;
		this.Num = num;
		this.isArray = isArray;
		this.isParam = false;
	}

	public String getId() {
		return Id;
	}

	public void setId(String id) {
		Id = id;
	}

	public String getNum() {
		return Num;
	}

	public void setNum(String num) {
		Num = num;
	}

	public boolean isArray() {
		return isArray;
	}

	public void setArray(boolean isArray) {
		this.isArray = isArray;
	}

	public boolean isParam() {
		return isParam;
	}

	public void setParam(boolean isParam) {
		this.isParam = isParam;
	}

}
