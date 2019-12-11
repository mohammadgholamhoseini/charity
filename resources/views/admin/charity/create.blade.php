@extends('admin.layout.master')
@section('content')

    <section class="content-header">
        @include('admin.layout.form-errors')
        <h1>
            ایجاد دسته بندی جدید
        </h1>
        <div class="col-md-10 offset-md-1">
            <form method="POST" action="{{route('categories.store')}}">
                @csrf
                <div class="form-group">
                <label>عنوان</label>
                <input type="text" class="form-control" name="name">
                 </div>
        <div class="form-group">
            <label>نام سرپرست</label>
            <input type="text" class="form-control" name="owner">
        </div>
        <div class="form-group">
            <label>تعداد اعضا</label>
            <input type="number" class="form-control" name="members">
        </div>
        <div class="form-group">
            <label>توضیحات</label>
            <textarea class="form-control" name="description"></textarea>
        </div>
        <div class="form-group">
            <label>ایمیل</label>
            <input type="email" class="form-control" name="email">
        </div>
        <div class="form-group">
            <label>آدرس</label>
            <input type="text" class="form-control" name="address">
        </div>
        <div class="form-group">
            <label>شماره تلفن</label>
            <input type="text" class="form-control" name="phone">
        </div>
        <div class="form-group">
            <label>نام مستعار</label>
            <input type="text" class="form-control" name="slug">
        </div>
        <div class="form-group">
            <label>متا-توضیحات</label>
            <textarea class="form-control" name="meta_desc"></textarea>
        </div>

        <div class="form-group">
            <label>متا-کلمات</label>
            <textarea class="form-control" name="meta_key"></textarea>
        </div>
        <div class="form-group">
            <label>متا-عنوان</label>
            <textarea class="form-control" name="meta_key"></textarea>
        </div>
            </form>
        </div>
    </section>
@endsection
