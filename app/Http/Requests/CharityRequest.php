<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

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
            'phone'=>'required|numeric|min:11',
            'category_id'=>'required',
            'user_id'=>'required',
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
            'name.required'=>'لطفا نام دسته بندی مورد نظر را وارد نمایید.',
            'description.required'=>'لطفا توضیحات دسته بندی مورد نظر را وارد نمایید.',
            'meta_desc.required'=>'لطفا متا توضیحات دسته بندی مورد نظر را وارد نمایید.',
            'meta_key.required'=>'لطفا متا کلمات  دسته بندی مورد نظر را وارد نمایید.',
            'slug.unique'=>'این نام مستعار قبلا ثبت شده است'
        ];
    }
}
