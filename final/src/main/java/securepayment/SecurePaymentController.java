package securepayment;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import product.ProductDTO;
import product.ProductService;
import upload.UploadService;

@Controller
public class SecurePaymentController {
	
	@Autowired
	@Qualifier("productservice")
	ProductService productService;
	
	@Autowired
	@Qualifier("uploadservice")
	UploadService uploadService;
	
	@Autowired
	@Qualifier("securepaymentservice")
	SecurePaymentService securePaymetService;
	
	@RequestMapping("/getsecurepaymentform")
	public ModelAndView getPaymentForm(int product_num) {
		System.out.println(product_num);
		ModelAndView mv= new ModelAndView();
		//상품 하나 가져오는 모듈 써서 상품 정보 모델로 보내줄 것
		//지금은 임시 상품 모델
		ProductDTO dto = new ProductDTO();
		dto.setProduct_price(10);
		dto.setProduct_title("bts 스픽콘 부채");
		String product_image = "/images/미쿠(bc74cb8d-1e76-4e60-851d-8b2ea06c1e7a).jpeg";
		mv.addObject("productdto",dto);
		mv.addObject("user_money", 900000);
		mv.addObject("image_path", product_image);
		mv.setViewName("securepayment/securepaymentform");
		return mv;
	}
	
	@RequestMapping("/securepaymentprocess")
	public String securePaymentProcess(SecurePaymentDTO dto) {
		//user_num 세션 받아 처리할 것 지금은 임시설정
		dto.setUser_num(2);
		System.out.println(dto);
		int insert_result = securePaymetService.securePaymentProcess(dto);
		if(insert_result>0) {
			//해당유저 보유머니 - 결제 금액
			int insert_result2 = securePaymetService.minusUserMoney(dto);
		}
		//redirect 경로 설정 필요
		return null;
	}
	
	@RequestMapping("/getbillingnumform")
	public ModelAndView getBillingnumForm() {
		int user_num=1;
		List<Map<String,String>> sellinglist = securePaymetService.getMyProduct(user_num);
		ModelAndView mv= new ModelAndView();
		mv.addObject("sellinglist", sellinglist);
		mv.setViewName( "securepayment/seller");
		return mv;
		//ajax로 구현할거라 product_num은 ajax로 넘겨받음
	}
	
	// 운송장 번호 등록
	@ResponseBody
	@RequestMapping(value = "/registerbillingnumber", produces= {"application/json;charset=utf-8"})
	public String registerBillingNumber(String billing_number,int product_num) {
		System.out.println(billing_number);
		System.out.println(product_num);
		ProductDTO dto = new ProductDTO();
		dto.setBilling_number(billing_number);
		dto.setProduct_num(product_num);
		//product_info billing_number에 입력받은 billing_number로 바꿈
		int insert_result = securePaymetService.registerBillingNumber(dto);
		
		if(insert_result>0) { //payment_info seller_check true로 바꿈
			//user_num과 product_num이 같은 곳 seller_chekc true로 update
			SecurePaymentDTO securepaymentdto = new SecurePaymentDTO();
			securepaymentdto.setProduct_num(product_num);
			//securepaymentdto.setUser_num(2); // 로그인 완성시 세션으로 user_num찾아올 것 이거 잘못했다. user_num은 구매자임
			
			int insert_result2 = securePaymetService.updateSellerCheck(product_num);
			//성공적으로 업데이트 시 구매자 구매확인 버튼 활성화 되어야함. 
			if(insert_result2>0) {
				return "{\"result\" : \"success\"}";
			}
		}
		return "{\"result\" : \"fail\"}";
	}

	//구매자 - 구매 목록확인에서 각 상품별 구매버튼이 활성화 되있거나 안되어있는 상태다. 결제 DTO의 리스트를 뿌려주는 상태일것임
	@RequestMapping("/confirmpurchase")
	@ResponseBody
	public String updateBuyerCheck(int product_num) {
		//구매자 로그인 세션으로 user_num가져 올 것''
		int user_num=2;
		System.out.println(product_num);
		SecurePaymentDTO dto = new SecurePaymentDTO();
		dto.setUser_num(user_num);
		dto.setProduct_num(product_num);
		//payment_info buyer_check now() 업데이트 시켜주고
		securePaymetService.updateBuyerCheck(dto);
		// product_info product_sell true로 업뎃 시켜줌 
		// productService로하는게 나은지 ..?
		int result = securePaymetService.updateProductSell(product_num);
		if(result>0) {
			return "{\"result\" : \"success\"}";
		}
		return "{\"result\" : \"fail\"}";
	}
	
	@RequestMapping("/getpurchaselist")
	public ModelAndView getPurchaselist() {
		//구매자 로그인 세션으로 user_num가져 올 것''
		// 구매상품의 운송장등록 기간이 3일을 넘겼다 -> 환불 버튼 띄워줌 
		//버튼이 운송장등록 3일지나 환불받기 버튼 떠있다 
		int user_num = 2;
		List<Map<String,String>> purchaselist = securePaymetService.getPurchaselist(user_num);
		ModelAndView mv= new ModelAndView();
		mv.addObject("purchaselist", purchaselist);
		mv.setViewName("securepayment/buyer");
		return mv;
	}
	
	@RequestMapping("/refund")
	@ResponseBody
	public String refund(int pay_price) {
		System.out.println(pay_price);
		//지금 로그인되어있는 user_num 세션으로 받아옴
		int user_num = 2;
		SecurePaymentDTO dto = new SecurePaymentDTO();
		dto.setPay_price(pay_price);
		dto.setUser_num(user_num);
		int update_result = securePaymetService.refund(dto);
		if(update_result>0) {
			return "{\"result\" : \"success\"}";
		}
		return "{\"result\" : \"fail\"}";
		
	}
	
	@RequestMapping(value="/depositseller", produces= {"application/json;charset=utf-8"})
	public String depositToSeller(int pay_price) {
		System.out.println(pay_price);
		int user_num = 1;
		SecurePaymentDTO dto = new SecurePaymentDTO();
		dto.setPay_price(pay_price);
		dto.setUser_num(user_num);
		int update_result = securePaymetService.depositToSeller(dto);
		return "{\"result\" : \"success\"}";
	}
}