<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class CharityRequest extends FormRequest
{
    /**
     * Determine if the user is authorized to make this request.
     *
     * @return bool
     */
    public function authorize()
    {
        return true;
    }

    /**
     * Get the validation rules that apply to the request.
     *
     * @return array
     */
    public function prepareForValidation()
    {
        if ($this->input('slug')){
            $this->merge(['slug'=>make_slug($this->input('slug'))]);
        }else
        {
            $this->merge(['slug'=>make_slug($this->input('name'))]);
        }
    }
    public function rules()
    {
        return [
            'name'=>'required',
            'owner'=>'required',
            'members'=>'required|numeric',
            'email'=>'required',
            'address'=>'required',
            'phone'=>'required|numeric',
            'category_id'=>'required',
            'meta_title'=>'required',
            'description'=>'required',
            'meta_desc'=>'required',
            'meta_key'=>'required',
            'slug'=>Rule::unique('categories')->ignore(request()->category),
        ];
    }
    public function messages()
    {
        return [
            'name.required'=>'لطفا نام خیریه مورد نظر را وارد نمایید.',
            'description.required'=>'لطفا توضیحات  مورد نظر را وارد نمایید.',
            'meta_desc.required'=>'لطفا متا توضیحات مورد نظر را وارد نمایید.',
            'meta_key.required'=>'لطفا متا کلمات مورد نظر را وارد نمایید.',
            'meta_title.required'=>'لطفا متا عنوان مورد نظر را وارد نمایید.',
            'owner.required'=>'لطفا نام مالک مورد نظر را وارد نمایید.',
            'members.required'=>'لطفا تعداد اعضا مورد نظر را وارد نمایید.',
            'email.required'=>'لطفا ایمیل  مورد نظر را وارد نمایید.',
            'address.required'=>'لطفا آدرس  مورد نظر را وارد نمایید.',
            'phone.required'=>'لطفا شماره تماس مورد نظر را وارد نمایید.',
            'phone.numeric'=>'شماره تلفن فقط باید عدد باشد',
            'phone.min'=>'شماره تلفن کمتر از حد مجاز است',
            'phone.max'=>'شماره بیشتر از حد مجاز است',
            'category_id.required'=>'لطفا دسته بندی مورد نظر را مشخص نمایید.',
            'slug.unique'=>'این نام مستعار قبلا ثبت شده است',

        ];
    }
}
