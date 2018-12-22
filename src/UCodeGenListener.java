import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class UCodeGenListener extends MiniGoBaseListener {

	ParseTreeProperty<String> newTexts = new ParseTreeProperty<>();
	ArrayList<ASTNode> local_vars = new ArrayList<>();
	ArrayList<ASTNode> global_vars = new ArrayList<>();
	// ArrayList<String> global_assign = new ArrayList<>();

	private final String SPACE = "           ";

	private int local_var_num = 0;
	private int global_var_num = 0;
	private int local_array_size = 0;
	private int global_array_size = 0;
	private int local_offset = 1;
	private int global_offset = 1;
	private int label_num = 0;
	private int block_depth = 1;

	@Override
	public void exitProgram(MiniGoParser.ProgramContext ctx) {
		this.MakeUcodeFile(ctx);
	}

	@Override
	public void exitDecl(MiniGoParser.DeclContext ctx) {
		if (ctx.var_decl() != null) { // 전역 변수
			String var = newTexts.get(ctx.var_decl());
			newTexts.put(ctx, var);

		} else { // 함수
			String func = newTexts.get(ctx.fun_decl());
			newTexts.put(ctx, func);
		}
	}

	@Override
	public void exitVar_decl(MiniGoParser.Var_declContext ctx) {
		String var_decl = "";

		if (ctx.getChildCount() == 3) { // 전역 변수 한개 선언
			ASTNode node = new ASTNode(ctx.IDENT(0).getText(), "1 " + global_offset, false);

			var_decl += SPACE + "sym 1 " + global_offset + " 1" + "\n";

			global_vars.add(node);
			global_var_num++;
			global_offset++;
		}

		else if (ctx.getChildCount() == 5) { // 전역 변수 두개 선언
			ASTNode node = new ASTNode(ctx.IDENT(0).getText(), "1 " + global_offset, false);

			var_decl += SPACE + "sym 1 " + global_offset + " 1" + "\n";

			global_vars.add(node);
			global_var_num++;
			global_offset++;

			ASTNode node2 = new ASTNode(ctx.IDENT(1).getText(), "1 " + global_offset, false);

			var_decl += SPACE + "sym 1 " + global_offset + " 1" + "\n";

			global_vars.add(node2);
			global_var_num++;
			global_offset++;
		}

		else { // 전역 배열 선언
			global_array_size = Integer.parseInt(ctx.getChild(3).getText());
			ASTNode node = new ASTNode(ctx.IDENT(0).getText(), "1 " + global_offset, true);

			var_decl += SPACE + "sym 1 " + global_offset + " " + global_array_size + "\n";

			global_vars.add(node);
			global_var_num += global_array_size;
			global_offset += global_array_size;
		}

		newTexts.put(ctx, var_decl);
	}

	@Override
	public void exitFun_decl(MiniGoParser.Fun_declContext ctx) {
		int p2 = 2; // 블록 번호
		int p3 = 2; // 렉시칼 레벨
		String fun_decl = "";
		String id = ctx.IDENT().getText();
		String compound_stmt = newTexts.get(ctx.compound_stmt());

		fun_decl += id + SPACE.substring(id.length(), SPACE.length()) + "proc " + local_var_num + " " + p2 + " " + p3
				+ "\n";

		for (ASTNode node : local_vars) {
			if (!node.isArray()) {
				fun_decl += SPACE + "sym " + node.getNum() + " " + 1 + "\n";

			} else {
				fun_decl += SPACE + "sym " + node.getNum() + " " + local_array_size + "\n";
			}
		}

		fun_decl += compound_stmt;

		local_var_num = 0;
		local_array_size = 0;
		local_offset = 1;
		local_vars.clear();
		newTexts.put(ctx, fun_decl);

	}

	@Override
	public void exitParam(MiniGoParser.ParamContext ctx) {
		String param = "";
		ASTNode node = new ASTNode(ctx.IDENT().getText(), "2 " + local_offset, false);

		node.setParam(true);
		local_var_num++;
		local_offset++;

		if (ctx.getChildCount() == 4) {
			node.setArray(true);
		}

		local_vars.add(node);
		newTexts.put(ctx, param);
	}

	@Override
	public void exitLocal_decl(MiniGoParser.Local_declContext ctx) {
		String local_decl = "";

		if (ctx.getChildCount() == 3) { // 지역 변수 선언
			ASTNode node = new ASTNode(ctx.IDENT().getText(), "2 " + local_offset, false);

			local_vars.add(node);
			local_var_num++;
			local_offset++;

		} else { // 지역 배열 선언
			local_array_size = Integer.parseInt(ctx.getChild(3).getText());
			ASTNode node = new ASTNode(ctx.IDENT().getText(), "2 " + local_offset, true);

			local_vars.add(node);
			local_var_num += local_array_size;
			local_offset += local_array_size;
		}

		newTexts.put(ctx, local_decl);
	}

	@Override
	public void exitStmt(MiniGoParser.StmtContext ctx) {
		String stmt = newTexts.get(ctx.getChild(0));

		newTexts.put(ctx, stmt);
	}

	@Override
	public void exitExpr_stmt(MiniGoParser.Expr_stmtContext ctx) {
		String expr = newTexts.get(ctx.expr());

		newTexts.put(ctx, expr);
	}

	@Override
	public void enterCompound_stmt(MiniGoParser.Compound_stmtContext ctx) {
		this.block_depth++;
	}

	@Override
	public void exitCompound_stmt(MiniGoParser.Compound_stmtContext ctx) {
		String compound_stmt = "";

		for (MiniGoParser.Local_declContext ld : ctx.local_decl()) {
			compound_stmt += newTexts.get(ld);
		}

		for (MiniGoParser.StmtContext st : ctx.stmt()) {
			compound_stmt += newTexts.get(st);
		}

		this.block_depth--;
		newTexts.put(ctx, compound_stmt);
	}

	@Override
	public void exitAssign_stmt(MiniGoParser.Assign_stmtContext ctx) {
		String assign_stmt = "";

		if (ctx.getChildCount() == 9) {
			ASTNode node, node2;

			if (this.block_depth != 1) { // 지역 변수 2개 배정문
				node = new ASTNode(ctx.IDENT(0).getText(), "2 " + local_offset, false);

				assign_stmt += SPACE + "ldc " + ctx.LITERAL(0) + "\n";
				assign_stmt += SPACE + "str 2 " + local_offset + "\n";

				local_vars.add(node);
				local_var_num++;
				local_offset++;

				node2 = new ASTNode(ctx.IDENT(0).getText(), "2 " + local_offset, false);

				assign_stmt += SPACE + "ldc " + ctx.LITERAL(1) + "\n";
				assign_stmt += SPACE + "str 2 " + local_offset + "\n";

				local_vars.add(node2);
				local_var_num++;
				local_offset++;
			}

		} else if (ctx.getChildCount() == 5) {
			ASTNode node;

			if (this.block_depth != 1) { // 지역 변수 배정문
				node = new ASTNode(ctx.IDENT(0).getText(), "2 " + local_offset, false);

				assign_stmt += newTexts.get(ctx.expr(0));
				assign_stmt += SPACE + "str 2 " + local_offset + "\n";

				local_vars.add(node);
				local_var_num++;
				local_offset++;
			}

			// node = new ASTNode(ctx.IDENT(0).getText(), "1 " + global_offset, false);
			//
			// String assign = SPACE + "ldc " + ctx.LITERAL(0) + "\n";
			// assign += SPACE + "str 1 " + global_offset + "\n";
			// global_assign.add(assign);
			//
			// global_vars.add(node);
			// global_var_num++;
			// global_offset++;

		} else if (ctx.getChildCount() == 4) {
			ASTNode var = this.findNode(ctx.IDENT(0).getText());

			assign_stmt += newTexts.get(ctx.expr(0));
			assign_stmt += SPACE + "str " + var.getNum() + "\n";

		} else {
			ASTNode var = this.findNode(ctx.IDENT(0).getText());

			assign_stmt += newTexts.get(ctx.expr(0));
			assign_stmt += SPACE + "lda " + var.getNum() + "\n";
			assign_stmt += SPACE + "add" + "\n";
			assign_stmt += newTexts.get(ctx.expr(1)) + SPACE + "sti" + "\n";
		}

		newTexts.put(ctx, assign_stmt);

	}

	@Override
	public void exitIf_stmt(MiniGoParser.If_stmtContext ctx) {
		String if_stmt = "";

		String label1 = "$$" + label_num;
		if_stmt += newTexts.get(ctx.expr());
		if_stmt += SPACE + "fjp " + label1 + "\n";

		if (ctx.getChildCount() == 3) { // if 문
			label_num++;
			if_stmt += newTexts.get(ctx.compound_stmt(0));

			String label2 = "$$" + (label_num - 1);
			if_stmt += label2 + SPACE.substring(label2.length(), SPACE.length()) + "nop" + "\n";

		} else { // if else 문
			label_num++;
			if_stmt += newTexts.get(ctx.compound_stmt(0));

			String label2 = "$$" + (label_num - 1);
			if_stmt += label2 + SPACE.substring(label2.length(), SPACE.length()) + "nop" + "\n";
			if_stmt += newTexts.get(ctx.compound_stmt(1));
		}

		newTexts.put(ctx, if_stmt);
	}

	@Override
	public void exitFor_stmt(MiniGoParser.For_stmtContext ctx) {
		String for_stmt = "";

		String label1 = "$$" + label_num;
		for_stmt = label1 + SPACE.substring(label1.length(), SPACE.length()) + "nop" + "\n";
		label_num++;

		String label2 = "$$" + label_num;
		for_stmt += newTexts.get(ctx.expr());
		for_stmt += SPACE + "fjp " + label2 + "\n";

		for_stmt += newTexts.get(ctx.compound_stmt());
		for_stmt += SPACE + "ujp " + label1 + "\n";

		for_stmt += label2 + SPACE.substring(label2.length(), SPACE.length()) + "nop" + "\n";

		newTexts.put(ctx, for_stmt);

	}

	@Override
	public void exitReturn_stmt(MiniGoParser.Return_stmtContext ctx) {
		String return_stmt = "";

		if (ctx.getChildCount() == 4) { // 리턴 값이 두개
			return_stmt += newTexts.get(ctx.expr(0));
			return_stmt += newTexts.get(ctx.expr(1));
			return_stmt += SPACE + "retv" + "\n";

		} else if (ctx.getChildCount() == 2) { // 리턴 값 한개
			return_stmt += newTexts.get(ctx.expr(0));
			return_stmt += SPACE + "retv" + "\n";

		} else { // 리턴 값 없음
			return_stmt += SPACE + "ret" + "\n";
		}

		newTexts.put(ctx, return_stmt);
	}

	@Override
	public void exitExpr(MiniGoParser.ExprContext ctx) {
		String expr = "";

		if (ctx.getChildCount() == 1) {
			if (ctx.IDENT() == null) {
				expr += SPACE + "ldc " + ctx.LITERAL(0) + "\n";

			} else {
				ASTNode var = this.findNode(ctx.IDENT().getText());

				if (var.isArray())
					expr += SPACE + "lda " + var.getNum() + "\n";
				else
					expr += SPACE + "lod " + var.getNum() + "\n";
			}
		}

		else if (ctx.getChildCount() == 2) { // 단항 연산
			expr += newTexts.get(ctx.expr(0));

			ASTNode var = findNode(ctx.getChild(1).getText());

			if (ctx.getChild(0).getText().equals("-"))
				expr += SPACE + "neg" + "\n";

			else if (ctx.getChild(0).getText().equals("--")) {
				expr += SPACE + "dec" + "\n";
				expr += SPACE + "str " + var.getNum() + "\n";

			} else if (ctx.getChild(0).getText().equals("++")) {
				expr += SPACE + "inc" + "\n";
				expr += SPACE + "str " + var.getNum() + "\n";

			} else if (ctx.getChild(0).getText().equals("!"))
				expr += SPACE + "notop" + "\n";
		}

		else if (isBinaryOp(ctx)) { // 이항 연산
			expr += newTexts.get(ctx.expr(0)) + newTexts.get(ctx.expr(1));

			if (ctx.getChild(1).getText().equals("*"))
				expr += SPACE + "mult" + "\n";

			else if (ctx.getChild(1).getText().equals("/"))
				expr += SPACE + "div" + "\n";

			else if (ctx.getChild(1).getText().equals("%"))
				expr += SPACE + "mod" + "\n";

			else if (ctx.getChild(1).getText().equals("+"))
				expr += SPACE + "add" + "\n";

			else if (ctx.getChild(1).getText().equals("-"))
				expr += SPACE + "sub" + "\n";

			else if (ctx.getChild(1).getText().equals("=="))
				expr += SPACE + "eq" + "\n";

			else if (ctx.getChild(1).getText().equals("!="))
				expr += SPACE + "ne" + "\n";

			else if (ctx.getChild(1).getText().equals("<="))
				expr += SPACE + "le" + "\n";

			else if (ctx.getChild(1).getText().equals("<"))
				expr += SPACE + "lt" + "\n";

			else if (ctx.getChild(1).getText().equals(">="))
				expr += SPACE + "ge" + "\n";

			else if (ctx.getChild(1).getText().equals(">"))
				expr += SPACE + "gt" + "\n";

			else if (ctx.getChild(1).getText().equals("and"))
				expr += SPACE + "and" + "\n";

			else if (ctx.getChild(1).getText().equals("or"))
				expr += SPACE + "or" + "\n";
		}

		else if (isAssigninExpr(ctx)) { // 변수 배정문
			ASTNode var = this.findNode(ctx.getChild(0).getText());

			expr += newTexts.get(ctx.expr(0));
			expr += SPACE + "str " + var.getNum() + "\n";
		}

		else if (ctx.getChildCount() == 3 && !isBinaryOp(ctx) && !isAssigninExpr(ctx)) {
			if (ctx.expr() != null) { // 괄호문
				expr += newTexts.get(ctx.expr(0));

			} else { // LITERAL,LITERAL
				expr += SPACE + "ldc " + ctx.LITERAL(0) + "\n";
				expr += SPACE + "ldc " + ctx.LITERAL(1) + "\n";
			}
		}

		else if (ctx.getChildCount() == 4) {
			if (ctx.args() != null) { // 함수 호출
				expr += SPACE + "ldp" + "\n";
				expr += newTexts.get(ctx.args()) + SPACE + "call " + ctx.IDENT().getText() + "\n";

			} else { // 배열 값
				ASTNode var = findNode(ctx.IDENT().getText());

				expr += newTexts.get(ctx.expr(0));

				if (var.isArray()) {
					expr += SPACE + "lda " + var.getNum() + "\n";
					expr += SPACE + "add" + "\n";
					expr += SPACE + "ldi" + "\n";
				}
			}
		}

		else if (ctx.getChildCount() == 6) {
			if (ctx.args() != null) { // fmt
				expr += SPACE + "ldp" + "\n";

				String fmt = ctx.getChild(2).getText();

				if (fmt.equals("Println")) { // 출력문 : write
					if (newTexts.get(ctx.args()).equals("")) { // 라인 피드
						expr += SPACE + "call lf" + "\n";

					} else {
						expr += newTexts.get(ctx.args()) + SPACE + "call write" + "\n";
					}

				} else if (fmt.equals("Scanln")) { // 입력문
					String scan = newTexts.get(ctx.args());

					expr += scan.replace("lod", "lda");
					expr += SPACE + "call read" + "\n";
				}

			} else { // 배열 배정문
				ASTNode var = this.findNode(ctx.IDENT().getText());
				expr += newTexts.get(ctx.expr(0));

				if (var.isArray()) {
					expr += SPACE + "lda " + var.getNum() + "\n";
					expr += SPACE + "add" + "\n";
					expr += newTexts.get(ctx.expr(1)) + SPACE + "sti" + "\n";
				}

			}
		}

		newTexts.put(ctx, expr);
	}

	@Override
	public void exitArgs(MiniGoParser.ArgsContext ctx) {
		String args = "";

		for (MiniGoParser.ExprContext ec : ctx.expr()) {
			args += newTexts.get(ec);
		}

		newTexts.put(ctx, args);
	}

	private boolean isAssigninExpr(MiniGoParser.ExprContext ctx) {
		return ctx.getChildCount() == 3 && ctx.getChild(1).getText().equals("=") == true;
	}

	private boolean isBinaryOp(MiniGoParser.ExprContext ctx) {
		return ctx.getChildCount() == 3 && ctx.getChild(1).getText().equals("=") == false;
	}

	private ASTNode findNode(String txt) {
		ASTNode var;

		if (find_local_var(txt) != null)
			var = find_local_var(txt);
		else
			var = find_global_var(txt);

		return var;
	}

	private ASTNode find_local_var(String input) {
		ASTNode temp = null;

		for (ASTNode n : local_vars) {
			if (n.getId().equals(input)) {
				temp = n;
			}
		}
		return temp;
	}

	private ASTNode find_global_var(String input) {
		ASTNode temp = null;

		for (ASTNode n : global_vars) {
			if (n.getId().equals(input)) {
				temp = n;
			}
		}
		return temp;
	}

	private void MakeUcodeFile(MiniGoParser.ProgramContext ctx) {
		File file = new File("result.uco");

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			String result = "";
			for (MiniGoParser.DeclContext decl : ctx.decl()) {
				result += newTexts.get(decl);

				if (newTexts.get(decl) == newTexts.get(decl.fun_decl())) {
					result += SPACE + "end" + "\n";
				}
			}

			result += SPACE + "bgn " + global_var_num + "\n";

			// for (String s : global_assign) {
			// result += s;
			// }

			result += SPACE + "ldp" + "\n";
			result += SPACE + "call main" + "\n";
			result += SPACE + "end" + "\n";

			bw.write(result);
			System.out.println(result);

			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
